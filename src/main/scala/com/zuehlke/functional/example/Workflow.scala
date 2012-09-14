package com.zuehlke.functional.example

import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import akka.event.Logging
import akka.actor.{ 
  ActorSystem,
  Props
}
import akka.dispatch.{
	Promise,
	Future,
	Await
}
import com.zuehlke.functional.example.functions.{
  Validator,
  Discounter,
  Executor
}
import com.zuehlke.functional.example.model.{
  IncomingOrder,
  ValidOrder,
  OrderRecipe,
  OrderOk,
  OrderError
}
import com.zuehlke.functional.example.ui.Console.{
  OrderWriter,
  OrderReader
}


object Workflow extends App {

  val system = ActorSystem("Workflow")
  val validator = system.actorOf(Props[Validator])
  val discounter = system.actorOf(Props[Discounter])
  val executor = system.actorOf(Props[Executor])
  val log = Logging.getLogger(system, this)

  // Necessary to be able to create promises
  implicit val disp = system.dispatcher
  implicit val timeout = Timeout(500 seconds)

  // ---- Workflow start
  val workflow: IncomingOrder => Future[OrderRecipe] = { order =>
    for {
      valid_order <- validator.ask(order).mapTo[ValidOrder]
      discounted_order <- if (valid_order.total >= 100.0)
        discounter.ask(valid_order).mapTo[ValidOrder]
      else {
        log.info("Order value: {}", valid_order.total)
        Promise.successful(valid_order)
      }
      recipt <- executor.ask(discounted_order).mapTo[OrderOk]
    } yield recipt
  }
  // ----- Workfow end

  val user_output = system.actorOf(Props(new OrderWriter))
  val user_input = system.actorOf(Props(new OrderReader(user_output, workflow)))

  // ----- Wait for all actors to stop
  system.awaitTermination()
}

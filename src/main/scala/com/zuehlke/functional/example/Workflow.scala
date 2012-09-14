package com.zuehlke.functional.example

import akka.actor._
import akka.pattern.{
  ask,
  gracefulStop
}
import akka.util.duration._
import akka.util.Timeout
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
import akka.dispatch.Promise
import akka.dispatch.Future
import akka.event.Logging
import akka.dispatch.Await
import java.util.Date
import com.zuehlke.functional.example.model.IncomingOrder

case class WriteReceipt(val receipt: OrderOk)
case class WriteError(val error: String)

class OrderReader(val writer: ActorRef, val workflow: IncomingOrder => Future[OrderRecipe]) extends Actor {
  import scala.io.Source._

  case object ReadOrder

  val lines = stdin.getLines

  implicit val system = context.system
  implicit val disp = context.dispatcher

  override def preStart = self ! ReadOrder
  
  def receive = {
    case ReadOrder => {
      print(">")
      Promise.successful {
        val nextLine = lines.next

        if (nextLine == "q")
          gracefulStop(context.parent, 20 seconds)
        else {
          workflow(IncomingOrder(nextLine)).onComplete {
            case Right(receipt: OrderOk) => writer ! WriteReceipt(receipt)
            case Left(error) => writer ! WriteError(error.getLocalizedMessage)
          }
        }
      } map { _ =>
        self ! ReadOrder
      }
    }
  }
}

class OrderWriter extends Actor {
  override def receive = {
    case WriteReceipt(receipt) => {
      println("<< Order %s executed on %s" format (receipt.order, receipt.completed_on))
    }

    case WriteError(error) => {
      println("<< Error while processing: %s" format error)
    }
  }
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

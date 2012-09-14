package com.zuehlke.functional.example.functions

import akka.actor.ActorRef
import akka.actor.Actor
import akka.event.Logging
import akka.util.duration._
import com.zuehlke.functional.example.model.{
  ValidOrder,
  OrderRecipe,
  OrderOk
}
import java.util.Date

class Executor extends Actor {
  val log = Logging.getLogger(context.system, this)

  case class Done(order: ValidOrder, subject: ActorRef)

  override def receive = {
    case valid_order: ValidOrder => {
      log.info("Processing order {}", valid_order)
      context.system.scheduler.scheduleOnce(5 seconds, self, Done(valid_order, sender))
    }
    case Done(order, subject) => {
      log.info("Processing done")
      subject ! OrderOk(order, new Date)
    }
    case e => log.error("Unknown message {}", e)
  }
}
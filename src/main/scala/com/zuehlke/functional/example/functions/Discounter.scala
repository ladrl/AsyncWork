package com.zuehlke.functional.example.functions

import com.zuehlke.functional.example.model.ValidOrder
import akka.actor.Actor
import akka.event.Logging

class Discounter extends Actor {
  val log = Logging.getLogger(context.system, this)

  override def receive = {
    case valid_order: ValidOrder => {
      log.info("Calculating discount for total {}", valid_order.total)
      sender ! valid_order.copy(discount = Some(valid_order.total * 0.05))
    }
  }
}
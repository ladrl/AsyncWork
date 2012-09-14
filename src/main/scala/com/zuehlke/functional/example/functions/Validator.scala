package com.zuehlke.functional.example.functions

import akka.actor.Actor
import akka.util.duration._
import com.zuehlke.functional.example.model.{
  IncomingOrder,
  ValidOrder,
  Address,
  Item
}
import akka.event.Logging
import com.zuehlke.functional.example.model.ValidOrder
import akka.actor.ActorRef
import akka.actor.Failed
import akka.actor.Status.Failure

class Validator extends Actor {
  case class Done(val order: ValidOrder, val sender: ActorRef)

  val log = Logging.getLogger(context.system, this)

  override def receive = {
    case IncomingOrder(order_text) => {
      log.info("Incoming order '{}'", order_text)

      // Parse order here
      val Parser = """Send (\w+) (\d+) (\w+)s?""".r

      order_text match {
        case Parser(recipient, count, product) => {
          val order = ValidOrder(
            deliver_to = Address(recipient),
            items = List((Integer.parseInt(count), Item(product, 10d))))

          context.system.scheduler.scheduleOnce((order.items.map { t => t._1 }.foldLeft(0) { _ + _ }) / 5.0 seconds, self, Done(order, sender))
        }
        case invalid_str:String => {
          log.debug("Unable to parse '{}'", invalid_str)
          sender ! Failure(new Exception("Order '%s' is malformed" format invalid_str))
        }
      }
    }

    case Done(order, receipient) => receipient ! order
  }
}
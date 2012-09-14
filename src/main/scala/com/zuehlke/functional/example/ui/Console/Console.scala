package com.zuehlke.functional.example.ui.Console

import com.zuehlke.functional.example.model.IncomingOrder
import com.zuehlke.functional.example.model.OrderOk
import akka.actor.ActorRef
import akka.dispatch.Promise
import akka.actor.Actor
import akka.pattern.{
  gracefulStop
}
import akka.dispatch.Future
import akka.util.duration._
import com.zuehlke.functional.example.model.OrderRecipe

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

        if (nextLine == "q") {
          gracefulStop(context.parent, 20 seconds)
        }
        else {
          workflow(IncomingOrder(nextLine)).onComplete {
            case Right(receipt: OrderOk) => writer ! WriteReceipt(receipt)
            case Left(error) => writer ! WriteError(error.getLocalizedMessage)
          }
          self ! ReadOrder
        }
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
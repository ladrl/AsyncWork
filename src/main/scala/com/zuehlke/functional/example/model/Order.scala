package com.zuehlke.functional.example.model

import java.util.Date


case class Address(val name: String)
case class Item(val name: String, val price: Double)
    
sealed trait Order

case class IncomingOrder(val content: String) extends Order

case class ValidOrder(val deliver_to: Address, val items: Seq[(Int, Item)], val discount: Option[Double] = None) extends Order {
  lazy val total = (items.map{ t => val (count, item) = t; count * item.price }.foldLeft(0d) {_ + _}) - discount.getOrElse(0d)
}

trait OrderRecipe

case class OrderOk(val order: ValidOrder, val completed_on: Date) extends OrderRecipe
case class OrderError(val error: String) extends OrderRecipe
package de.esserjan.com.sol

import de.esserjan.com.catalog

object Amount {
  val PIECES = "pieces"
  val VOLUMES = "volumes"
}
sealed trait Amount
case class Pieces(n:Int) extends Amount
case class Volume(l:BigDecimal) extends Amount

trait Cart extends Iterable[catalog.Product] {
  def amount(p:catalog.Product):Option[Amount]
}
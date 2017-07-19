package de.esserjan.com.sol

import java.util.Locale

import de.esserjan.com.catalog
import de.esserjan.com.sol.persist.CartEntry

object Currency {
  def toJavaCurrency(c: Currency): java.util.Currency =
    java.util.Currency.getInstance(c.iso)

  def currencyForInput(iso: String): Currency =
    currencyForJava(
      java.util.Currency.getInstance(iso))

  def currencyForLocale(loc: Locale): Currency =
    currencyForJava(
      java.util.Currency.getInstance(loc))

  def currencyForJava(cur: java.util.Currency): Currency =
    Currency(cur.getCurrencyCode)
}

case class Currency(iso: String)

object Price {
  val ZERO = new Price {
    override def as(cur: Currency): BigDecimal = java.math.BigDecimal.ZERO
  }
}

trait Price {
  def as(cur: Currency): BigDecimal
}

trait PriceImplicits {
  def c: Currency

  val ZERO: BigDecimal = java.math.BigDecimal.ZERO

  implicit def toBigDecimal(p: Price) = p.as(c)
}

case class Offer(product: catalog.Product, price: Price)
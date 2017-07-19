package de.esserjan.com.checkout

import de.esserjan.com.sol._
import de.esserjan.com.sol.service.{OfferFacade, PromotionFacade}

object OrderStatus extends Enumeration {
  val PROPOSED, ACCEPTED, PAID, DELIVERING, FULFILLED = Value
}

case class OrderEntry(offer: Offer,
                      // amount:Amount,
                      promos: Seq[Promotion],
                      status: OrderStatus.Value = OrderStatus.PROPOSED)

trait Tax {
  def name: String

  def rate: BigDecimal
}

object Order {
  private[Order] val ZERO: BigDecimal = java.math.BigDecimal.ZERO
}

case class Order(entries: Iterable[OrderEntry], cartPromos: Iterable[Promotion]) {
  def status: OrderStatus.Value =
    entries.groupBy(_.status).head._1

  // TODO every entry should declare to which taxGroup it belongs
  def totalPrice(c: Currency): BigDecimal = {
    implicit def toPriceCurrency(p: Price): BigDecimal = p.as(c)

    val entriesTotal = entries.foldLeft(Order.ZERO) {
      case (sum, e) =>
        val entryPromoTotal = e.promos.foldLeft(Order.ZERO) {
          case (promoSum, p: ProductPromotion) => promoSum + p.price
          case (promoSum, p: OfferPromotion) => promoSum + p.price
        }

        sum + e.offer.price + entryPromoTotal
    }

    val cartPromosTotal = cartPromos.foldLeft(Order.ZERO) {
      case (cartPromoSum, cp: CartPromotion) => cartPromoSum + cp.price
    }

    entriesTotal + cartPromosTotal
  }

  def taxesIncluded(c: Currency): Map[Tax, BigDecimal] = // TODO extract product taxes
    Map()
}


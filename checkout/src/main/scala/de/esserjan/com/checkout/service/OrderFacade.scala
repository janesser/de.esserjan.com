package de.esserjan.com.checkout.service

import de.esserjan.com.checkout.{OrderEntry, Order}
import de.esserjan.com.service.Facade
import de.esserjan.com.sol.service.{OfferFacade, PromotionFacade}
import de.esserjan.com.sol.{ExclusivePromotionImplicits, ExclusivePromotionOrdering, Cart, Currency}

import scala.util.Sorting

case class OrderFacade(of: OfferFacade, pf: PromotionFacade) extends Facade with ExclusivePromotionImplicits {
  def accept(cart: Cart, c: Currency): Order = {

    val entries: Iterable[OrderEntry] = cart map {
      p =>
        of.getBestPriceOffer(p, c) match {
          case Some(o) =>
            val productPromos = pf.getBestPromotions(p)
            val offerPromos = pf.getBestPromotions(o)

            Some(OrderEntry(o, productPromos ++ offerPromos))
          case None =>
            None
        }
    } filter {
      _.isDefined
    } map {
      case Some(e) => e
    }

    val cartPromos = pf.getBestPromotions(cart)
    Order(
      entries,
      cartPromos)
  }

  def acquire(o: Order): Order = ???

  def deliver(o: Order): Order = ???

  def fulfilled(o: Order): Order = ???
}

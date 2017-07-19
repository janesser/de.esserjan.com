package de.esserjan.com.sol

import de.esserjan.com.catalog

sealed trait Promotion {
  def price: Price
}

case class ProductPromotion(product: catalog.Product, override val price: Price) extends Promotion

case class CartPromotion(cart: Cart, override val price: Price) extends Promotion

case class OfferPromotion(offer: Offer, override val price: Price) extends Promotion

case class PriceOrdering(c: Currency) extends Ordering[Price] with PriceImplicits {
  override def compare(x: Price, y: Price): Int = y.compare(x)
}

case class ExclusivePromotionOrdering(c:Currency) extends Ordering[ExclusivePromotion] with PriceImplicits {

  def sumUpPromos(promos: Seq[Promotion]): BigDecimal =
    promos.foldLeft(ZERO) {
      case (promoSum, p: Promotion) =>
        promoSum + p.price
    }

  override def compare(x: ExclusivePromotion, y: ExclusivePromotion): Int = {
    val xTotal = sumUpPromos(x.promotions)
    val yTotal = sumUpPromos(y.promotions)

    yTotal.compare(xTotal)
  }
}

case class ExclusivePromotion(promotions: Seq[Promotion]) extends Promotion {
  def price: Price = new Price {
    def as(c: Currency): BigDecimal =
      promotions.foldLeft(Price.ZERO.as(c)) {
        case (promoSum, p) => promoSum + p.price.as(c)
      }
  }
}

trait ExclusivePromotionImplicits {
  // TODO contextualize currency
  implicit val ord = ExclusivePromotionOrdering(Currency.currencyForLocale(java.util.Locale.getDefault()))

  def bestExclusivePromo(promos: Seq[ExclusivePromotion]): Option[ExclusivePromotion] =
    promos.sorted.headOption

  def bestExclusivePromoPromos(promos: Seq[ExclusivePromotion]): Seq[Promotion] =
    bestExclusivePromo(promos) match {
      case Some(p) => p.promotions
      case None => Seq()
    }
}
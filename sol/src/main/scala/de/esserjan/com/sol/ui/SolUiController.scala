package de.esserjan.com.sol.ui

import java.util.Locale

import de.esserjan.com.catalog.persist
import de.esserjan.com.catalog.persist.ProductsDao
import de.esserjan.com.catalog.ui.CatalogUiSupport
import de.esserjan.com.persist.NamedDao
import de.esserjan.com.sol.service.{OfferFacade, PromotionFacade}
import de.esserjan.com.sol.{ExclusivePromotion, Price}
import de.esserjan.com.ui.CommonUiController
import de.esserjan.com.{catalog, sol}

object SolUiSupport {

  case class Offer(price: BigDecimal, currencySymbol: String)

  sealed trait UiPromotion

  case class UiProductPromotion(product: catalog.Product, price: Price) extends UiPromotion

}

trait SolUiSupport {
  self: CommonUiController =>

  import de.esserjan.com.sol.ui.SolUiSupport._

  def toUiOffer(offer: sol.Offer, currency: sol.Currency, loc: Locale): Offer =
    Offer(offer.price.as(currency), sol.Currency.toJavaCurrency(currency).getSymbol(loc))

  def toUiPromotion(promotion: sol.Promotion): UiPromotion = promotion match {
    case sol.ProductPromotion(product, price) =>
      UiProductPromotion(product, price)
    case _ => ???
  }

  def toUiPromotions(promotions: Seq[sol.Promotion]): Seq[UiPromotion] = {
    import scala.language.postfixOps
    promotions map {
      case ExclusivePromotion(promotions) =>
        toUiPromotions(promotions)
      case p => List(toUiPromotion(p))
    } flatten
  }
}

case class SolUiController(offerFacade: OfferFacade,
                           promotionFacade: PromotionFacade,
                           productsDao: NamedDao[catalog.IdType, persist.Product, persist.ProductName] = ProductsDao)
  extends CommonUiController("sol")
  with CatalogUiSupport
  with SolUiSupport {

  trait SolUiAction {
    implicit val userLocales = SolUiController.this.userLocales

    val productId = params("productId")

    implicit val loc = SolUiController.this.locale

    val cur = params.get("currencyId") match {
      case Some(currencyId) =>
        sol.Currency.currencyForInput(currencyId)
      case None =>
        sol.Currency.currencyForLocale(loc)
      /*
      TODO could use userLocales here
      TODO consider currency to cookie dropdown in UI
      */
    }
  }

  get("/offers/:productId") {
    new SolUiAction {
      val product = productsDao.findByIdWithName(productId) match {
        case Some((p, names)) =>
          layoutTemplate(view("offers"),
            TITLE ->
              (messages("offers") + s" $productId"),
            "product" ->
              toUiProduct(p, names, productNameFallback),
            "offers" ->
              (offerFacade.getAllOffers(p) map {
                o => toUiOffer(o, cur, loc)
              }))
        case None => resourceNotFound()
      }
    }
  }

  get("/offers/:productId/best") {
    new SolUiAction {
      val product = productsDao.findByIdWithName(productId) match {
        case Some((p, names)) =>
          offerFacade.getBestPriceOffer(p, cur) match {
            case Some(bestOffer) =>
              layoutTemplate(view("bestOffer"),
                TITLE ->
                  (messages("bestOffer") + s" $productId"),
                "product" ->
                  toUiProduct(p, names, productNameFallback),
                "bestOffer" ->
                  toUiOffer(bestOffer, cur, loc))
            case None => resourceNotFound()
          }
        case None => resourceNotFound()
      }
    }
  }

  get("/promotions/p:productId") {
    new SolUiAction {
      val product = productsDao.findByIdWithName(productId) match {
        case Some((p, names)) =>
          val promotions = promotionFacade.getExclusivePromotions(p)
          if (!promotions.isEmpty) {
            layoutTemplate(view("promotions"),
              TITLE ->
                (messages("promotions") + s" $productId"),
              "product" ->
                toUiProduct(p, names, productNameFallback),
              "promotions" ->
                toUiPromotions(promotions))
          } else
            resourceNotFound()
        case None =>
          resourceNotFound()
      }
    }
  }

  get("/promotions/o:offerId") {
    resourceNotFound() // TODO
  }

  get("/promotions/c:cartId") {
    resourceNotFound() // TODO
  }
}

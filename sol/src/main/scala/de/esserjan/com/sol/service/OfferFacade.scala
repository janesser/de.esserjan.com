package de.esserjan.com.sol.service

import akka.actor.ActorRef
import akka.util.Timeout
import de.esserjan.com.service.ActorFacade
import de.esserjan.com.sol.actor.{OfferClaim, OfferClaimResponse}
import de.esserjan.com.{catalog, sol}

case class OfferFacade(override val consumer: ActorRef)
                      (override implicit val timeout: Timeout)
  extends ActorFacade {

  def getBestPriceOffer(product: catalog.Product,
                        currency: sol.Currency): Option[sol.Offer] =
    getBestOffer(product, (o1, o2) =>
      o1.price.as(currency) <= o2.price.as(currency))

  protected def getBestOffer(product: catalog.Product,
                             lessThan: (sol.Offer, sol.Offer) => Boolean): Option[sol.Offer] =
    getAllOffers(product).sortWith(lessThan).headOption

  def getAllOffers(product: catalog.Product): Seq[sol.Offer] =
    await(OfferClaim(product)) {
      resp: OfferClaimResponse =>
        resp.offers
    }
}
package de.esserjan.com.sol.service

import akka.actor.ActorRef
import akka.util.Timeout
import de.esserjan.com.catalog
import de.esserjan.com.service.ActorFacade
import de.esserjan.com.sol.actor.{Promoted, PromotionClaim, PromotionClaimResponse}
import de.esserjan.com.sol._

case class PromotionFacade(override val consumer: ActorRef)
                          (override implicit val timeout: Timeout)
  extends ActorFacade
  with ExclusivePromotionImplicits {

  protected def promoted(p: Option[catalog.Product] = None,
                         c: Option[Cart] = None,
                         o: Option[Offer] = None): Promoted =
    Promoted(p, c, o)

  def getBestPromotions(p: catalog.Product): Seq[Promotion] =
    bestExclusivePromoPromos(getExclusivePromotions(p))

  def getBestPromotions(c: Cart): Seq[Promotion] =
    bestExclusivePromoPromos(getExclusivePromotions(c))

  def getBestPromotions(o: Offer): Seq[Promotion] =
    bestExclusivePromoPromos(getExclusivePromotions(o))

  def getExclusivePromotions(p: catalog.Product): Seq[ExclusivePromotion] =
    getExclusivePromotions(promoted(p = Some(p)))


  def getExclusivePromotions(c: Cart): Seq[ExclusivePromotion] =
    getExclusivePromotions(promoted(c = Some(c)))

  def getExclusivePromotions(o: Offer): Seq[ExclusivePromotion] =
    getExclusivePromotions(promoted(o = Some(o)))

  protected def getExclusivePromotions(p: Promoted): Seq[ExclusivePromotion] =
    await(PromotionClaim(p)) {
      resp: PromotionClaimResponse =>
        resp.promotions
    }
}

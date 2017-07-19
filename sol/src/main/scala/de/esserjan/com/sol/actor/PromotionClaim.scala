package de.esserjan.com.sol.actor

import akka.actor.{Actor, ActorRef}
import de.esserjan.com.actor.{BidderMessage, CommonPollActor}
import de.esserjan.com.catalog
import de.esserjan.com.sol.{Cart, ExclusivePromotion, Offer, Promotion}
import org.joda.time.{DateTime, Duration}

import scala.concurrent.ExecutionContext

sealed trait PromotionMessage

case class Promoted(p: Option[catalog.Product], c: Option[Cart], o: Option[Offer])

// client messages

case class PromotionClaim(ref: Promoted) extends PromotionMessage

case class PromotionClaimResponse(ref: Promoted, promotions: Seq[ExclusivePromotion]) extends PromotionMessage

// broadcast messages

case class PromotionBidderClaim(ref: Promoted) extends PromotionMessage

case class PromotionBidderResponse(override val ref: Promoted,
                                   promotions: Seq[Promotion],
                                   validUntil: DateTime)
  extends PromotionMessage
  with BidderMessage[Promoted, ExclusivePromotion] {

  override def result = ExclusivePromotion(promotions)
}

// internal scheduler message type

case class PromotionPollTimeout(ref: Promoted, sender: ActorRef) extends PromotionMessage


trait PromotionBidder extends Actor

object PromotionConsumer {
  val PROMOTION_BROADCAST = "promotionBroadcast"

  val DEFAULT_POLL_DURATION = Duration.standardSeconds(1L)

  val DEFAULT_MINIMUM_LASTING = Duration.standardMinutes(1L)
}

case class PromotionConsumer(override val pollDuration: Duration = PromotionConsumer.DEFAULT_POLL_DURATION,
                             override val minimumLasting: Duration = PromotionConsumer.DEFAULT_MINIMUM_LASTING)
                            (override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global)
  extends CommonPollActor {
  override type BC_MSG = PromotionBidderClaim
  override type TO_MSG = PromotionPollTimeout
  override type REF = Promoted
  override type RESULT = ExclusivePromotion
  override type BID_MSG = PromotionBidderResponse

  override val broadcastName = "promotionBroadcast"

  override def receive: Receive = {
    case PromotionClaim(ref) =>
      broadcastClaim(PromotionBidderClaim(ref), PromotionPollTimeout(ref, sender()))
    case resp: PromotionBidderResponse =>
      bufferBidderResponse(resp)
    case PromotionPollTimeout(ref, sender) =>
      respondAndClean(ref) {
        promotions: Seq[ExclusivePromotion] =>
          sender ! PromotionClaimResponse(ref, promotions)
      }
  }
}
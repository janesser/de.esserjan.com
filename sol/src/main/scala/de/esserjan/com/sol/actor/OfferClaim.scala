package de.esserjan.com.sol.actor

import akka.actor.{Actor, ActorRef}
import de.esserjan.com.actor.{BidderMessage, CommonPollActor}
import de.esserjan.com.catalog
import de.esserjan.com.sol.Offer
import org.joda.time.{DateTime, Duration}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

sealed trait OfferMessage

// client messages

case class OfferClaim(product: catalog.Product) extends OfferMessage

case class OfferClaimResponse(product: catalog.Product, offers: Seq[Offer]) extends OfferMessage

// broadcast messages

case class OfferBidderClaim(product: catalog.Product) extends OfferMessage

case class OfferBidderResponse(offer: Offer,
                                override val validUntil: DateTime) extends OfferMessage with BidderMessage[catalog.Product, Offer] {
  override def ref = offer.product
  override def result = offer
}

// internal scheduler message type

case class OfferPollTimeout(product: catalog.Product, sender: ActorRef) extends OfferMessage

trait OfferBidder extends Actor

object OfferConsumer {
  val NAME = "offerConsumer"

  val DEFAULT_POLL_DURATION = Duration.standardSeconds(1L)

  val DEFAULT_MINIMUM_LASTING = Duration.standardMinutes(1L)
}

case class OfferConsumer(override val pollDuration: Duration = OfferConsumer.DEFAULT_POLL_DURATION,
                         override val minimumLasting: Duration = OfferConsumer.DEFAULT_MINIMUM_LASTING)
                        (implicit override val ec: ExecutionContext = scala.concurrent.ExecutionContext.global)
  extends CommonPollActor {
  override type BC_MSG = OfferBidderClaim
  override type TO_MSG = OfferPollTimeout
  override type REF = catalog.Product
  override type RESULT = Offer
  override type BID_MSG = OfferBidderResponse

  override val broadcastName = "offerBroadcast"

  private val offerResponses = mutable.HashMap[catalog.Product, mutable.Buffer[OfferBidderResponse]]()

  override def receive: Actor.Receive = {
    case OfferClaim(product) =>
      broadcastClaim(OfferBidderClaim(product), OfferPollTimeout(product, sender()))
    case response: OfferBidderResponse =>
      bufferBidderResponse(response)
    case OfferPollTimeout(product, sender) =>
      respondAndClean(product) {
        offers:Seq[Offer] =>
          sender ! OfferClaimResponse(product, offers)
      }
  }
}

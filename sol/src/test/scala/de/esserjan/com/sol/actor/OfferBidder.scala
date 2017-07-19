package de.esserjan.com.sol.actor

import akka.actor.Actor
import de.esserjan.com.sol.{Currency, Price, Offer}
import org.joda.time.DateTime

class RandomOfferBidder extends OfferBidder {
  val random = new java.util.Random(1L)

  override def receive: Actor.Receive = {
    case OfferBidderClaim(p) =>
      sender ! OfferBidderResponse(Offer(p, new Price {
        override def as(cur:Currency): BigDecimal =
          BigDecimal(random.nextDouble())
      }), DateTime.now.plusDays(5))
  }
}

object MuteOfferBidder extends OfferBidder {
  override def receive: Actor.Receive = {
    case OfferClaim(product) => // NOOP
  }
}
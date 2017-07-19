package de.esserjan.com.sol.actor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.routing._
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import de.esserjan.com.catalog
import de.esserjan.com.sol.service.OfferFacade
import de.esserjan.com.util.AkkaUtil
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, fixture}

import scala.concurrent.{ExecutionContext, Await}

@RunWith(classOf[JUnitRunner])
class OfferConsumerTests
  extends TestKit(ActorSystem(classOf[OfferConsumerTests].getSimpleName))
  with fixture.FlatSpecLike
  with MockitoSugar
  with Matchers {

  val randomOfferBidder = TestActorRef(
    Props(classOf[RandomOfferBidder])
  )

  val offerBroadcastRef = system.actorOf(
    BroadcastGroup(List(
      system.actorSelection(randomOfferBidder.path).pathString
    )).props(),
    "testOfferBroadcast")

  val offerConsumer = TestActorRef(
    Props(new OfferConsumer() {
      override def broadcast = offerBroadcastRef
    })
  )

  case class FixtureParam(product: catalog.Product)

  override def withFixture(test: OneArgTest) = {
    val p = mock[catalog.Product]

    super.withFixture(test.toNoArgTest(FixtureParam(p)))
  }

  implicit val timeout: Timeout = Timeout(2L, TimeUnit.SECONDS)

  "OfferConsumer" should "send message to broadcast group" in { p => val product = p.product
    AkkaUtil.await[OfferClaimResponse](offerConsumer, OfferClaim(product)) match {
      case OfferClaimResponse(product, offers) =>
        offers.isEmpty should be(java.lang.Boolean.FALSE)
    }
  }

  it should "register a new bidder" in { p => val product = p.product
    val bidder2 = TestActorRef(Props(classOf[RandomOfferBidder]))
    val bidder3 = TestActorRef(Props(MuteOfferBidder))

    try {
      offerBroadcastRef ! AddRoutee(ActorRefRoutee(bidder2))
      offerBroadcastRef ! AddRoutee(ActorRefRoutee(bidder3))

      Await.result(offerBroadcastRef ? GetRoutees, timeout.duration) match {
        case Routees(routees) =>
          routees should have(
            length(3)
          )
      }

      AkkaUtil.await[OfferClaimResponse](offerConsumer, OfferClaim(product)) match {
        case OfferClaimResponse(product, offers) =>
          offers should have(
            length(2)
          )
      }
    } finally {
      offerBroadcastRef ! RemoveRoutee(ActorRefRoutee(bidder2))
      offerBroadcastRef ! RemoveRoutee(ActorRefRoutee(bidder3))
      Await.result(offerBroadcastRef ? GetRoutees, timeout.duration) match {
        case Routees(routees) =>
          routees should have(
            length(1)
          )
      }
    }
  }

  "OfferFacade" should "instrument OfferConsumer" in { p => val product = p.product
    val offerFacade = OfferFacade(offerConsumer)

    offerFacade.getAllOffers(product) should have(
      length(1)
    )
  }
}

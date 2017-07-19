package de.esserjan.com.checkout.bootstrap

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.util.Timeout
import de.esserjan.com.bootstrap.{ModuleSetup, ModuleDefiner}
import de.esserjan.com.checkout.service
import de.esserjan.com.persist.DaoReference
import de.esserjan.com.service.FacadeFactory
import de.esserjan.com.sol.bootstrap.SolModule
import de.esserjan.com.sol.service.{PromotionFacade, OfferFacade}

object CheckoutModule {
  val ORDER_FACADE = "orderFacade"
}

class CheckoutModule extends ModuleDefiner {

  import CheckoutModule._

  override def toModuleSetup = new ModuleSetup {
    override def facadeSetup(actors: Map[String, ActorRef], daos: Map[String, DaoReference]): Map[String, FacadeFactory] = {
      implicit val timeout = Timeout(1L, TimeUnit.SECONDS)

      Map(ORDER_FACADE -> FacadeFactory {
        () => service.OrderFacade(
          new OfferFacade(actors(SolModule.OFFER_CONSUMER)),
          new PromotionFacade(actors(SolModule.PROMOTION_CONSUMER)))
      })
    }
  }
}

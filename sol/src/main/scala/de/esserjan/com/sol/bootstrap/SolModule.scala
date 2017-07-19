package de.esserjan.com.sol.bootstrap

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.util.Timeout
import de.esserjan.com.bootstrap.{ModuleDefiner, ModuleSetup, SquerylModuleSetup}
import de.esserjan.com.persist.DaoReference
import de.esserjan.com.service.FacadeFactory
import de.esserjan.com.sol.{actor, persist, service, ui}
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.Swagger
import org.squeryl.Schema

trait Facade

object SolModule {
  val CARTS_DAO = "cartsDao"

  val OFFER_CONSUMER = "offerConsumer"
  val OFFER_FACADE = "offerFacade"

  val PROMOTION_CONSUMER = "promotionConsumer"
  val PROMOTION_FACADE = "promotionFacade"
}

class SolModule extends ModuleDefiner {

  import de.esserjan.com.sol.bootstrap.SolModule._

  override def toModuleSetup: ModuleSetup = new SquerylModuleSetup {
    override protected def schemas: Seq[Schema] = List(persist.SolSchema)

    override def daoSetup(): Map[String, DaoReference] =
      Map(CARTS_DAO -> DaoReference(persist.CartsDao))

    override def actorSetup(daos: Map[String, DaoReference])
                           (implicit system: ActorSystem): Map[String, ActorRef] =
      Map(
        OFFER_CONSUMER -> system.actorOf(Props(new actor.OfferConsumer())),
        PROMOTION_CONSUMER -> system.actorOf(Props(new actor.PromotionConsumer()))
      )

    override def facadeSetup(actors: Map[String, ActorRef],
                             daos: Map[String, DaoReference]): Map[String, FacadeFactory] = {
      implicit val timeout = Timeout(1L, TimeUnit.SECONDS)

      Map(
        OFFER_FACADE -> FacadeFactory {
          () => new service.OfferFacade(actors(OFFER_CONSUMER))
        },
        PROMOTION_FACADE -> FacadeFactory {
          () => new service.PromotionFacade(actors(PROMOTION_CONSUMER))
        }
      )
    }

    override def controllerSetup(facades: Map[String, FacadeFactory],
                                 daos: Map[String, DaoReference])
                                (implicit swagger: Swagger): Map[String, ScalatraServlet] =
      Map("sol" ->
        new ui.SolUiController(
          facades(OFFER_FACADE).create[service.OfferFacade](),
          facades(PROMOTION_FACADE).create[service.PromotionFacade]()))
  }
}

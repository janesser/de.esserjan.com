package de.esserjan.com.bootstrap

import akka.actor.{ActorRef, ActorSystem}
import de.esserjan.com.persist.DaoReference
import de.esserjan.com.service.FacadeFactory
import gnieh.sohva.async.Database
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{ApiInfo, Swagger}

object ModuleDefiner {
  val swagger = new Swagger(
    Swagger.SpecVersion, // swaggerVersion
    "1", // apiVersion
    ApiInfo(
      "Commercial platform", // title
      "Operations about catalog, offers and purchases", // description
      "", // termsOfServiceUrl
      "JEsser@gmx.de", // contact
      "", // license
      "" // licenseUrl
    ))
}

trait ModuleDefiner {
  def toModuleSetup: ModuleSetup
}

trait ModuleSetup {
  def afterSessionFactorySet(initializeSchema: Boolean): Unit = {}

  def couchSetup(createDatabase:Boolean)(implicit system: ActorSystem): Map[String, Database] = Map.empty

  def daoSetup(couchDbs:Map[String, Database]): Map[String, DaoReference] = Map.empty

  def daoSetup(): Map[String, DaoReference] = Map.empty

  def actorSetup(daos: Map[String, DaoReference])
                (implicit system: ActorSystem): Map[String, ActorRef] = Map.empty

  def facadeSetup(actors: Map[String, ActorRef],
                  daos: Map[String, DaoReference]): Map[String, FacadeFactory] = Map.empty

  def controllerSetup(facades: Map[String, FacadeFactory],
                      daos: Map[String, DaoReference])
                     (implicit swagger: Swagger): Map[String, ScalatraServlet] = Map.empty
}
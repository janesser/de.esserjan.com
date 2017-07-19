package de.esserjan.com.bootstrap

import akka.actor.ActorSystem
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.Swagger

trait ModulesRegistry {
  import scala.language.postfixOps
  protected val modules:Iterable[ModuleSetup] =
    scala.collection.JavaConversions.asScalaIterator(
      java.util.ServiceLoader.load(classOf[ModuleDefiner]).iterator
    ) map {
      _.toModuleSetup
    } toList

  private[bootstrap] def bootstrap(initSchema:Boolean)(implicit system:ActorSystem, swagger:Swagger):Map[String, ScalatraServlet] = {
    modules.foreach(_.afterSessionFactorySet(initSchema))
    val couchDbs = modules.map(_.couchSetup(initSchema)).flatten.toMap
    val daos = modules.map(m => m.daoSetup() ++ m.daoSetup(couchDbs)).flatten.toMap
    val actors = modules.map(_.actorSetup(daos)).flatten.toMap
    val facades = modules.map(_.facadeSetup(actors, daos)).flatten.toMap
    modules.map(_.controllerSetup(facades, daos)).flatten.toMap
  }
}

object ModulesRegistry extends ModulesRegistry

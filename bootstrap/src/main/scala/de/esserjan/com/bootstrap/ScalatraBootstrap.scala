package de.esserjan.com.bootstrap

import javax.servlet.ServletContext

import _root_.akka.actor.ActorSystem
import org.scalatra._
import org.scalatra.swagger._

trait VerboseDebug {
  val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  def printJvmProps(verbose: Boolean): Unit = {
    if (verbose) {
      ClassLoader
        .getSystemClassLoader
        .asInstanceOf[java.net.URLClassLoader]
        .getURLs.foreach {
        u => logger.info(u.toExternalForm)
      }
      logger.info(System.getProperty("user.dir"))
    }
  }
}

class ResourcesApp(implicit val swagger: Swagger)
  extends ScalatraServlet
  with JacksonSwaggerBase

class ScalatraBootstrap extends LifeCycle with VerboseDebug {

  implicit val swagger = ModuleDefiner.swagger

  implicit val system = ActorSystem()

  var dbSupport: SquerylDatasourceSupport = null

  override def init(context: ServletContext): Unit = {
    printJvmProps(java.lang.Boolean.parseBoolean(context.getInitParameter("verbose")))

    val initSchema =
      context.getInitParameter("db.initSchema") match {
        case null => true
        case s: String => java.lang.Boolean.parseBoolean(s)
      }

    trait WebXmlDataSourceConfig {
      val adapterClass =
        context.getInitParameter("db.adapterClass") match {
          case null => Class.forName("org.squeryl.adapters.H2Adapter") // default
          case s: String => Class.forName(s)
        }
    }

    dbSupport = context.getInitParameter("db.support") match {
      case "dbcp2" =>
        new DbcpSupport with WebXmlDataSourceConfig
      case _ => // default
        new HikariCpSupport with WebXmlDataSourceConfig
    }
    dbSupport.prepareDataSource()

    ModulesRegistry.bootstrap(initSchema) foreach {
      case (path, controller) =>
        context.mount(controller, s"/$path", path)
    }

    context.mount(new ResourcesApp, "/api-docs")
  }

  override def destroy(context: ServletContext): Unit = {
    dbSupport.shutdown()
    org.squeryl.SessionFactory.concreteFactory = None
  }
}

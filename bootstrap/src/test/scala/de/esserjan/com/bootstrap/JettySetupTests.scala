package de.esserjan.com.bootstrap

import org.eclipse.jetty.server._
import org.eclipse.jetty.server.handler._
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatest._
import org.scalatra.servlet.ScalatraListener
import org.scalatra.test.HttpComponentsClient
import org.squeryl.adapters.DerbyAdapter

class JettySetupTests extends FlatSpec with Matchers {

  "Jetty" should "start with default settings without exception" in {
    new ConfigFunTest
  }


  it should "start with apache dbcp and derby without exception" in {
    new ConfigFunTest(Some("dbcp2"), Some(classOf[DerbyAdapter].getName))
  }
}

case class HttpClient(implicit val server: Server) extends HttpComponentsClient {
  override def baseUrl: String =
    server.getConnectors collectFirst {
      case conn: ServerConnector =>
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        require(port > 0, "The detected local port is < 1, that's not allowed")
        "http://%s:%d".format(host, port)
    } getOrElse sys.error("can't calculate base URL: no connector")
}

case class ConfigFunTest(dbSupportParam: Option[String] = None,
                         dbAdapterClassParam: Option[String] = None,
                         serverPort:Int = 0,
                         shouldStop:Boolean = true) extends Matchers {
  implicit val server = new Server(serverPort)
  val handlers = new HandlerList
  val webApp = new WebAppContext
  webApp.addServlet(classOf[DefaultServlet], "/")
  webApp.setResourceBase("src/main/webapp")

  webApp.setInitParameter(ScalatraListener.LifeCycleKey, classOf[ScalatraBootstrap].getName)
  dbSupportParam match {
    case Some(dbSupport) =>
      webApp.setInitParameter("db.support", dbSupport)
    case None =>
  }
  dbAdapterClassParam match {
    case Some(dbAdapterClass) =>
      webApp.setInitParameter("db.adapterClass", dbAdapterClass)
    case None =>
  }

  webApp.addEventListener(new ScalatraListener)

  handlers.setHandlers(Array(
    new ResourceHandler,
    webApp))

  server.setHandler(handlers)

  server.start()

  webApp.isStarted should equal(true)
  webApp.isThrowUnavailableOnStartupException should equal(false)

  new HttpClient {
    get("/api-docs") {
      status should be(200)
    }
    get("/api-docs/categories") {
      status should be(200)
    }
    get("/categories") {
      status should be(200)
    }
  }

  if (shouldStop)
    server.stop()
}
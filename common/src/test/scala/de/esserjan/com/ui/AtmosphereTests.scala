package de.esserjan.com.ui

import java.net.URI
import java.util.concurrent.{CountDownLatch, TimeUnit}

import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}
import org.eclipse.jetty.websocket.client.{ClientUpgradeRequest, WebSocketClient}
import org.json4s.{DefaultFormats, Formats}
import org.scalatest.FlatSpec
import org.scalatra.atmosphere._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.scalate.ScalateSupport
import org.scalatra.test.scalatest.ScalatraSuite
import org.scalatra.{ScalatraServlet, SessionSupport}

object AtmosphereTests {

  class EchoController
    extends ScalatraServlet
    with ScalateSupport
    with JacksonJsonSupport
    with SessionSupport
    with AtmosphereSupport {
    implicit val ec = scala.concurrent.ExecutionContext.global

    override protected implicit val jsonFormats: Formats = DefaultFormats

    atmosphere("/echo") {
      new AtmosphereClient {
        override def receive: AtmoReceive = {
          case msg: TextMessage => send(msg)
        }
      }
    }
  }

}

class AtmosphereTests extends FlatSpec with ScalatraSuite {

  import de.esserjan.com.ui.AtmosphereTests._

  addServlet(new EchoController, "/server")

  override def baseUrl: String =
    server.getConnectors collectFirst {
      case conn: ServerConnector =>
        val host = Option(conn.getHost) getOrElse "localhost"
        val port = conn.getLocalPort
        "ws://%s:%d".format(host, port)
    } getOrElse sys.error("can't calculate base URL: no connector")

  case class webSocket(onConnect: Session => Unit,
                       onText: String => Unit) extends WebSocketAdapter {
    override def onWebSocketConnect(session: Session): Unit = {
      println(s"connect: $session")
      super.onWebSocketConnect(session)
      onConnect(session)
    }

    override def onWebSocketText(message: String): Unit = {
      println(s"text: $message")
      super.onWebSocketText(message)
      onText(message)
    }

    override def onWebSocketError(cause: Throwable): Unit = {
      println(s"error: $cause")
      super.onWebSocketError(cause)
    }

    override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
      println(s"close: $statusCode $reason")
      super.onWebSocketClose(statusCode, reason)
    }
  }

  "EchoController" should "echo test message" in {
    // http://www.websocket.org/echo.html
    val uri = URI.create(s"$baseUrl/server/echo")
    println(s"server-uri: $uri")

    val client = new WebSocketClient()
    val upgradeRequest = new ClientUpgradeRequest()

    try {
      client.start()
      var echoed = false
      client.connect(
        webSocket(
          s => s.getRemote.sendString("Hello there!"),
          msg => echoed = true),
        uri, upgradeRequest)
      new CountDownLatch(1).await(5, TimeUnit.SECONDS)
      echoed should equal(true)
    } finally {
      client.stop()
    }
  }
}
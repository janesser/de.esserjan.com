package de.esserjan.com.catalog.ui

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.atmosphere.{AtmoReceive, AtmosphereClient, AtmosphereSupport}
import org.scalatra.{SessionSupport, ScalatraServlet}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.scalate.ScalateSupport

object CatalogAtmosphereController {

  // TODO refactor medias as document attachment
  @Deprecated
  trait UrlAuthority[-AddressableEntityType] {
    def url(entity: AddressableEntityType): java.net.URL
  }

  @Deprecated
  trait Media {
    self =>
    implicit def urlAuthority: UrlAuthority[self.type]

    def url: java.net.URL =
      urlAuthority.url(this)
  }

}

class CatalogAtmosphereController
  extends ScalatraServlet
  with ScalateSupport
  with JacksonJsonSupport
  with SessionSupport
  with AtmosphereSupport {

  implicit val ec = scala.concurrent.ExecutionContext.global

  override protected implicit val jsonFormats: Formats = DefaultFormats

  atmosphere("/catalog_updates") {
    new AtmosphereClient {
      override def receive: AtmoReceive = {
        case _ => ??? // TODO
      }
    }
  }
}

package de.esserjan.com.catalog.rest

import javax.servlet.http.HttpServletRequest

import de.esserjan.com.catalog.persist
import de.esserjan.com.common
import de.esserjan.com.persist.CommonLanguage
import de.esserjan.com.rest.DatabaseSupport
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.i18n.I18nSupport
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.swagger.SwaggerSupport
import org.scalatra.{ActionResult, NotFound, ScalatraBase, ScalatraServlet}

abstract class CatalogController(override val applicationDescription: String)
  extends ScalatraServlet
  with JacksonJsonSupport
  with SwaggerSupport
  with JValueResult
  with DatabaseSupport {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    bindSession(request)
  }

  after() {
    unbindSession(request)
  }
}

trait LanguageReferencing extends I18nSupport {
  self: ScalatraBase =>

  def languageId(implicit request: HttpServletRequest): String =
    persist.Language.languageForLocale(locale).id
}

trait ProductReferencing {
  self: ScalatraBase =>

  def notFoundProduct(productId: persist.Product.IdType): ActionResult =
    NotFound(s"No product for referencing ID $productId")

  def notFoundLanguage(languageId: persist.Language.IdType): ActionResult =
    NotFound(s"No language for referencing ID $languageId")
}
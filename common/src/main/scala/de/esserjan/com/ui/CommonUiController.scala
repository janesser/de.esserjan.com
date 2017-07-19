package de.esserjan.com.ui

import java.util.Locale

import de.esserjan.com.persist
import de.esserjan.com.rest.DatabaseSupport
import org.scalatra.ScalatraServlet
import org.scalatra.i18n.Messages
import org.scalatra.scalate.ScalateI18nSupport

object CommonUiController {
  def getNameByUserLocales(entityNames: Iterable[persist.EntityName[_]])
                          (implicit userLocales: Array[java.util.Locale]): Option[String] = {
    import scala.language.postfixOps
    val namesByLang: Map[persist.CommonLanguage, persist.EntityName[_]] =
      entityNames.groupBy(_.languageId) map {
        case (langId, names) =>
          persist.CommonLanguage.languageForInput(langId) -> names.head
      } toMap

    userLocales map {
      loc => persist.CommonLanguage.languageForLocale(loc)
    } map {
      lang =>
        namesByLang.get(lang)
    } filter {
      nameOpt =>
        nameOpt.isDefined
    } map {
      nameOpt =>
        nameOpt.get.name
    } headOption
  }
}

class CommonUiController(bundlePath: String) extends ScalatraServlet with ScalateI18nSupport with DatabaseSupport {

  val TITLE = "title"

  override val defaultLayoutPath = Some("/layouts/default.scaml")

  override def provideMessages(locale: Locale): Messages = Messages(locale, s"i18n/$bundlePath")

  before() {
    contentType = "text/html"
    bindSession(request)
  }

  after() {
    unbindSession(request)
  }

  def view(viewName: String) = s"/templates/views/$viewName.scaml"

  def getNameByUserLocales(entityNames: Iterable[persist.EntityName[_]])
                          (implicit userLocales: Array[java.util.Locale]): Option[String] =
    CommonUiController.getNameByUserLocales(entityNames)(userLocales)
}

package de.esserjan.com.ui

import de.esserjan.com.persist.{CommonLanguage, EntityName}
import org.scalatest.{FlatSpec, Matchers}
import org.squeryl.dsl.ManyToOne

class CommonUiControllerTest extends FlatSpec with Matchers {

  import de.esserjan.com.ui.CommonUiController._

  case class TestEntityName(name: String,
                            languageId: String,
                            entityId: String) extends EntityName[String] {
    override type L = CommonLanguage

    override def language: ManyToOne[L] = ???
  }
  
  def testName(name:String = "mockedEntityName",
               loc:java.util.Locale = java.util.Locale.GERMANY) =
    TestEntityName(name, CommonLanguage.languageForLocale(loc).id, null)

  "CommonUiController" should "get name with exact language match" in {
    val loc = java.util.Locale.GERMANY

    getNameByUserLocales(List(testName(name="the name", loc = loc)))(Array(loc)) should be(Some("the name"))
  }

  it should "get name out of two" in {
    val loc = java.util.Locale.GERMANY

    val name = getNameByUserLocales(
      List(testName(loc=java.util.Locale.FRANCE),
        testName(name="the name", loc=loc)))(Array(loc))
    name.isDefined should equal(true)
    name.get should equal("the name")
  }

  it should "get name respecting locale order" in {
    val locales = Array(java.util.Locale.FRANCE, java.util.Locale.GERMANY)

    getNameByUserLocales(List(testName(loc = locales(1)), testName("the name", locales(0))))(locales) should equal(Some("the name"))
  }

  it should "get name by secondary locale" in {
    val locales = Array(java.util.Locale.FRANCE, java.util.Locale.GERMANY)

    getNameByUserLocales(List(testName(loc = java.util.Locale.ITALY), testName("the name", locales.tail.head)))(locales) should equal(Some("the name"))
  }

}

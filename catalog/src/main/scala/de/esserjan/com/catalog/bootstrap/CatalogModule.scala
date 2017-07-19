package de.esserjan.com.catalog.bootstrap

import java.net.URL

import de.esserjan.com.bootstrap.{CouchDbPropsModuleSetup, ModuleDefiner, SquerylModuleSetup}
import de.esserjan.com.catalog.persist.CatalogSchema
import de.esserjan.com.catalog.{persist, rest, ui}
import de.esserjan.com.persist.DaoReference
import de.esserjan.com.service.FacadeFactory
import gnieh.sohva.async.{Database, CouchClient}
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.Swagger

object CatalogModule {
  val COUCH_DB = "catalogCouchDb"

  val CATEGORIES_DAO = "categoriesDao"

  val PRODUCTS_DAO = "productsDao"

  val LANGUAGES_DAO = "languagesDao"

  val DESCRIPTIONS_DAO = "descriptionsDao"
}

trait CatalogModuleSetup extends SquerylModuleSetup with CouchDbPropsModuleSetup

class CatalogModule extends ModuleDefiner {

  import CatalogModule._

  override def toModuleSetup = new CatalogModuleSetup {
    override def schemas = List(CatalogSchema)

    override def propsPath: String = "catalog_couch.properties"

    override def couchDatabases()(implicit client: CouchClient): Map[String, Database] =
      Map(COUCH_DB -> client.database("catalogCouchDb"))

    override def daoSetup(couchDbs:Map[String, Database]): Map[String, DaoReference] =
      Map(
        CATEGORIES_DAO -> persist.CategoriesDao,
        PRODUCTS_DAO -> persist.ProductsDao,
        LANGUAGES_DAO -> persist.LanguagesDao,
        DESCRIPTIONS_DAO -> new persist.ProductDescriptionsDaoImpl(couchDbs("catalogCouchDb"))
      ) map {
        case (id,dao) =>
          id -> DaoReference(dao)
      }

    override def controllerSetup(facades: Map[String, FacadeFactory],
                                 daos: Map[String, DaoReference])
                                (implicit swagger: Swagger): Map[String, ScalatraServlet] =
      Map("categories" ->
        new rest.CategoriesController(
          daos(CATEGORIES_DAO).dao[persist.CategoriesDao],
          daos(PRODUCTS_DAO).dao[persist.ProductsDao]),
        "products" -> new rest.ProductsController(
          daos(PRODUCTS_DAO).dao[persist.ProductsDao]
        ),
        "productdescriptions" -> new rest.ProductDescriptionsController(
          daos(DESCRIPTIONS_DAO).dao[persist.ProductDescriptionsDao],
          daos(PRODUCTS_DAO).dao[persist.ProductsDao],
          daos(LANGUAGES_DAO).dao[persist.LanguagesDao]
        ),
        "catalog" -> new ui.CatalogUiController)
  }
}

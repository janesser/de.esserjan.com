package de.esserjan.com.catalog.rest

import de.esserjan.com.catalog
import de.esserjan.com.catalog.persist
import org.scalatra._
import org.scalatra.swagger.{StringResponseMessage, Swagger}

object ProductsController {

  import scala.language.{implicitConversions, postfixOps}

  case class Product(id: catalog.IdType,
                     names: Map[String, String])

  def id(): catalog.IdType = System.currentTimeMillis().toString

  implicit def restProduct(p: persist.Product): Product =
    restProduct(p, List.empty[persist.ProductName])

  implicit def restProduct(p: persist.Product, names: Iterable[persist.ProductName]): Product =
    Product(p.id,
      names map {
        n =>
          n.languageId ->
            n.name
      } toMap)

  implicit def persistenceProduct(prod: Product): (persist.Product, Iterable[persist.ProductName]) = {
    val p = persist.Product(prod.id)
    val n: Iterable[persist.ProductName] = prod.names map {
      case (lang, name) =>
        persist.ProductName(
          prod.id,
          lang,
          name)
    }

    (p, n)
  }

  def notFoundProduct(productId: String): ActionResult =
    NotFound(s"No product with ID $productId")
}

class ProductsController(dao: persist.ProductsDao = persist.ProductsDao)
                        (implicit val swagger: Swagger)
  extends CatalogController("Operations about products.") {

  import javax.servlet.http.HttpServletResponse._

import de.esserjan.com.catalog.rest.ProductsController._

  val getProduct = apiOperation[Option[Product]]("getProduct")
    .parameter(pathParam[catalog.IdType]("id").description("ID of the product").required)
    .responseMessage(StringResponseMessage(SC_NOT_FOUND, "Unknown product."))
    .summary("Get a product by its ID")
  get("/:id", operation(getProduct)) {
    val id = params("id")
    dao.findByIdWithName(id) match {
      case Some((p, names)) => restProduct(p, names)
      case None => notFoundProduct(id)
    }
  }

  val putProduct = apiOperation[Unit]("putProduct")
    .parameter(bodyParam[Product]("product"))
    .summary("Put a product")
  put("/", operation(putProduct)) {
    val prod = parsedBody.extract[Product]
    persistenceProduct(prod) match {
      case (p, names) =>
        dao.saveWithNames(p, names)
    }

  }

  val deleteProduct = apiOperation[Unit]("deleteProduct")
    .parameter(pathParam[catalog.IdType]("id").description("ID of the product").required)
    .responseMessage(StringResponseMessage(SC_NOT_FOUND, "Unknown product."))
    .summary("Delete a product by its ID")
  delete("/:id", operation(deleteProduct)) {
    val id = params("id")
    if (!dao.delete(id)) notFoundProduct(id)
  }
}
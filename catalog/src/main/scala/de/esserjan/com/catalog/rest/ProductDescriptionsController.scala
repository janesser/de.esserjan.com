package de.esserjan.com.catalog.rest

import java.io.InputStream

import de.esserjan.com.catalog
import de.esserjan.com.catalog.persist
import de.esserjan.com.persist.CommonLanguage
import de.esserjan.com.rest.IdGenerator
import org.scalatra.swagger.{StringResponseMessage, Swagger}
import org.scalatra.{ActionResult, NotFound}

object ProductDescriptionsController extends IdGenerator {

  type ProductDescriptionDocument = persist.ProductDescriptionDocument

  def notFound(productId: catalog.IdType,
               languageId: CommonLanguage.IdType): ActionResult =
    NotFound(s"No description document for productId $productId and languageId $languageId")

  def notFoundAttachment(productId: catalog.IdType,
                         languageId: CommonLanguage.IdType,
                         attachmentId: String): ActionResult =
    NotFound(s"No attachment $attachmentId at description document for productId $productId and languageId $languageId")
}

class ProductDescriptionsController(dao: persist.ProductDescriptionsDao,
                                    productsDao: persist.ProductsDao,
                                    languagesDao: persist.LanguagesDao)
                                   (implicit val swagger: Swagger)
  extends CatalogController("Operations about product description documents.")
  with LanguageReferencing
  with ProductReferencing {

  import de.esserjan.com.catalog.rest.ProductDescriptionsController._
  import org.scalatra.util.io.copy

  val getProductDescriptionById = apiOperation[Option[persist.ProductDescriptionDocument]]("getProductDescriptionById")
    .parameter(pathParam[Long]("productId").description("ID of the product").required)
    .parameter(headerParam[String]("Accept-Language").description("Language tag of requested product description").optional)
    .responseMessage(StringResponseMessage(404, "Unknown product."))
    .responseMessage(StringResponseMessage(404, "Unavailable language."))
    .responseMessage(StringResponseMessage(404, "No product description."))
    .summary("Get a product description by product ID")
  get("/:productId", operation(getProductDescriptionById)) {
    val productId = params("productId")
    val langId = languageId
    languagesDao.findById(langId) match {
      case Some(_) =>
        productsDao.findById(productId) match {
          case Some(_) =>
            val docId = persist.ProductDescriptionDocument.documentId(productId, langId)
            dao.findById(docId) match {
              case Some(doc) => doc
              case None => notFound(docId, langId)
            }
          case None => notFoundProduct(productId)
        }
      case None => notFoundLanguage(langId)
    }
  }

  val putProductDescription = apiOperation[Unit]("putProductDescription")
    .parameter(bodyParam[ProductDescriptionDocument]("productDescription"))
    .responseMessages(StringResponseMessage(404, "Unknown product."))
    .responseMessages(StringResponseMessage(404, "Unavailable language."))
    .summary("Put a product description")
  put("/", operation(putProductDescription)) {
    val doc = parsedBody.extract[ProductDescriptionDocument]
    val langId = languageId
    languagesDao.findById(langId) match {
      case Some(_) =>
        productsDao.findById(doc.productId) match {
          case Some(_) =>
            dao.save(doc)
          case None =>
            notFoundProduct(doc.productId)
        }
      case None => notFoundLanguage(langId)
    }
  }

  val deleteProductDescription = apiOperation[Unit]("deleteProductDescription")
    .parameter(pathParam[String]("productDescriptionDocId").description("ID of the product description").required)
    .summary("Delete a product text description by its ID")
  delete("/:docId", operation(deleteProductDescription)) {
    dao.delete(params("docId"))
  }

  val postAttachment = apiOperation[Unit]("postDescriptionAttachment")
    .parameter(pathParam[String]("productId").description("ID of the product described").required)
    .parameter(pathParam[String]("attachmentId").description("ID of product description attachment").required)
    .parameter(bodyParam[InputStream]("productDescriptionDoc"))
    .parameter(headerParam[String]("Accept-Language").description("Language tag for product description").optional)
    .summary("Add an description attachment.")
  post("/:productId/a:attachmentId") {
    val langId = languageId
    val productId = params("productId")

    val docId = persist.ProductDescriptionDocument.documentId(productId, langId)
    dao.findById(docId) match {
      case Some(_) =>
        val is = parsedBody.extract[InputStream]
        dao.saveAttachment(
          docId,
          params("attachmentId"),
          is,
          contentType)
      case None =>
        notFound(productId, languageId)
    }
  }

  val getAttachment = apiOperation[catalog.Binary]("getDescriptionAttachment")
    .parameter(pathParam[String]("productId").description("ID of the product described").required)
    .parameter(pathParam[String]("attachmentId").description("ID of product description attachment").required)
    .parameter(headerParam[String]("Accept-Language").description("Language tag for product description").optional)
    .summary("Get a description attachment.")

  get("/:productId/a:attachmentId") {
    val langId = languageId
    val productId = params("productId")

    val docId = persist.ProductDescriptionDocument.documentId(productId, langId)
    val attachmentId = params("attachmentId")
    dao.getAttachment(docId, attachmentId) match {
      case Some((mimeType, inputStream)) =>
        contentType = mimeType
        copy(inputStream, response.getOutputStream)
      case None => notFoundAttachment(productId, langId, attachmentId)
    }
  }

  val deleteAttachment = apiOperation[Unit]("deleteDescriptionAttachment")
    .parameter(pathParam[String]("productId").description("ID of the product described").required)
    .parameter(pathParam[String]("attachmentId").description("ID of product description attachment").required)
    .summary("Delete a description attachment")
  delete("/:productId/a:attachmentId") {
    val langId = languageId
    val productId = params("productId")

    val docId = persist.ProductDescriptionDocument.documentId(productId, langId)
    val attachmentId = params("attachmentId")
    dao.deleteAttachment(docId, attachmentId)
  }
}
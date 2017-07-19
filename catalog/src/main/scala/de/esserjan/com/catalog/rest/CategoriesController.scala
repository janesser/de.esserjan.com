package de.esserjan.com.catalog.rest

import javax.servlet.http.HttpServletRequest

import de.esserjan.com.catalog
import de.esserjan.com.catalog.persist
import org.scalatra.swagger.{StringResponseMessage, Swagger}
import org.scalatra.{ActionResult, NotFound}

object CategoriesController {

  import scala.language.implicitConversions

  case class Category(id: Long,
                      names: Map[String, String],
                      parentId: Option[Long] = None)

  implicit def persistentCategory(cat: Category): (persist.Category, Iterable[persist.CategoryName]) = {
    val c = persist.Category(cat.id, cat.parentId)
    val names = cat.names map {
      case (lang, name) =>
        persist.CategoryName(cat.id,
          persist.Language.languageForInput(lang).id,
          name)
    }

    (c, names)
  }

  implicit def restCategory(c: persist.Category): Category =
    restCategory(c, List.empty[persist.CategoryName])

  implicit def restCategory(c: persist.Category,
                            names: Iterable[persist.CategoryName]): Category = {
    import scala.language.postfixOps
    val restNames = names map {
      n =>
        n.languageId -> n.name
    }
    Category(c.id,
      restNames.toMap,
      c.parentId)
  }

  def notFoundCategory(categoryId: Long): ActionResult =
    NotFound(s"No category with ID $categoryId")
}

class CategoriesController(val dao: persist.CategoriesDao = persist.CategoriesDao,
                           val productsDao: persist.ProductsDao = persist.ProductsDao)
                          (implicit val swagger: Swagger)
  extends CatalogController("Operations about product categories.")
  with ProductReferencing {

  import de.esserjan.com.catalog.rest.CategoriesController._

  private def categoryId(implicit request: HttpServletRequest): Long =
    params("categoryId").toLong

  private def subCategoryId(implicit request: HttpServletRequest): Long =
    params("subCategoryId").toLong

  private def productId(implicit request: HttpServletRequest): catalog.IdType =
    params("productId")

  val getRootCategories = apiOperation[List[Category]]("getRootCategories")
    .summary("Get all root categories.")
  get("/", operation(getRootCategories)) {
    dao.findRootCategories map {
      case (c, names) => restCategory(c, names)
    }
  }

  val getCategory = apiOperation[Category]("getCategory")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .responseMessage(StringResponseMessage(404, "Unknown category."))
    .summary("Get a category.")
  get("/:categoryId", operation(getCategory)) {
    dao.findByIdWithName(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some((c, names)) => restCategory(c, names)
    }
  }

  val putCategory = apiOperation[Unit]("putCategory")
    .parameter(bodyParam[Category]("category").required)
    .summary("Put a category.")
  put("/", operation(putCategory)) {
    val c = parsedBody.extract[Category]
    persistentCategory(c) match {
      case (cat, names) =>
        dao.saveWithNames(cat, names)
    }
  }

  val deleteCategory = apiOperation[Unit]("deleteCategory")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .summary("Delete a category.")
  delete("/:categoryId", operation(deleteCategory)) {
    dao.delete(categoryId)
  }

  val getSubCategories = apiOperation[List[Category]]("getSubCategories")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .summary("Get sub-categories of a given category.")
  get("/:categoryId/subcategories", operation(getSubCategories)) {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) => c.subCategories map {
        c => restCategory(c)
      }
    }
  }

  val postCategoryRelation = apiOperation[Unit]("postSubCategory")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .parameter(pathParam[Long]("subCategoryId").description("ID of the sub-category").required)
    .responseMessage(StringResponseMessage(404, "Category not found."))
    .responseMessage(StringResponseMessage(404, "Sub-category not found."))
    .summary("Post sub-category relation to a given category.")
  post("/:categoryId/subCategories/:subCategoryId", operation(postCategoryRelation)) {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) =>
        dao.findById(subCategoryId) match {
          case None => notFoundCategory(categoryId)
          case Some(subCat) =>
            dao.associateSubCategory(c, subCat)
        }
    }
  }

  val deleteCategoryRelation = apiOperation[Unit]("deleteCategoryRelation")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .parameter(pathParam[Long]("subCategoryId").description("ID of the sub-category").required)
    .responseMessage(StringResponseMessage(404, "Category not found."))
    .responseMessage(StringResponseMessage(404, "Sub-category not found."))
    .summary("Delete sub-category relation to a given category.")
  delete("/:categoryId/subCategories/:subCategoryId", operation(deleteCategoryRelation)) {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) =>
        dao.findById(subCategoryId) match {
          case None => notFoundCategory(subCategoryId)
          case Some(subCat) =>
            dao.disassociateSubCategory(c, subCat)
        }
    }
  }

  val getProducts = apiOperation[List[Product]]("getProducts ")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .responseMessage(StringResponseMessage(404, "Category not found."))
    .summary("Get products related to given category.")
  get("/:categoryId/products") {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) =>
        dao.findProducts(c)
    }
  }

  val postProductRelation = apiOperation[Unit]("postProductRelation")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .parameter(pathParam[Long]("productId").description("ID of the product").required)
    .responseMessage(StringResponseMessage(404, "Category not found."))
    .responseMessage(StringResponseMessage(404, "Product not found."))
    .summary("Post product relation to given category.")
  post("/:categoryId/products/:productId") {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) =>
        productsDao.findById(productId) match {
          case None => notFoundProduct(productId)
          case Some(p) =>
            dao.associateProduct(c, p)
        }
    }
  }

  val deleteProductRelation = apiOperation[Unit]("deleteProductRelation")
    .parameter(pathParam[Long]("categoryId").description("ID of the category").required)
    .parameter(pathParam[Long]("productId").description("ID of the product").required)
    .responseMessage(StringResponseMessage(404, "Category not found."))
    .responseMessage(StringResponseMessage(404, "Product not found."))
    .summary("Delete product relation to given category.")
  delete("/:categoryId/products/:productId") {
    dao.findById(categoryId) match {
      case None => notFoundCategory(categoryId)
      case Some(c) =>
        productsDao.findById(productId) match {
          case None => notFoundProduct(productId)
          case Some(p) =>
            dao.dissociateProduct(c, p)
        }
    }
  }
}

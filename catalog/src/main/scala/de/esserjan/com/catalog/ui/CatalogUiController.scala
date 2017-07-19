package de.esserjan.com.catalog.ui

import de.esserjan.com.catalog.persist
import de.esserjan.com.catalog.ui.CatalogUiSupport.Category
import de.esserjan.com.ui.CommonUiController

object CatalogUiSupport {

  case class Category(id: persist.Category.IdType,
                      name: String,
                      parentId: Option[persist.Category.IdType] = None)

  case class Product(id: persist.Product.IdType,
                     name: String,
                     categoryId: Option[Long] = None)
}

trait CatalogUiSupport {
  self: CommonUiController =>

  import de.esserjan.com.catalog.ui.CatalogUiSupport._

  def categoryNameFallback(categoryId: persist.Category.IdType): String =
    messages("category") + s" $categoryId"

  def toUiCategory(c: persist.Category,
                   names: Iterable[persist.CategoryName],
                   fallback: Long => String)
                  (implicit userLocales: Array[java.util.Locale]): Category =
    Category(c.id,
      getNameByUserLocales(names)
        .getOrElse(fallback(c.id)),
      c.parentId)

  def toUiCategories(categories: Iterable[(persist.Category, Iterable[persist.CategoryName])],
                     fallback: Long => String)
                    (implicit userLocales: Array[java.util.Locale]): Iterable[Category] =
    categories map {
      case (c, names) =>
        toUiCategory(c, names, fallback)
    }

  def productNameFallback(productId: persist.Product.IdType): String =
    messages("product") + s" $productId"

  def toUiProduct(p: persist.Product,
                  names: Iterable[persist.ProductName],
                  fallback: String => String)(implicit userLocales: Array[java.util.Locale]): Product = {

    val name: Option[String] = getNameByUserLocales(names)

    val categoryId = p.category match {
      case Some(c) => Some(c.id)
      case None => None
    }

    Product(p.id, name.getOrElse(fallback(p.id)), categoryId)
  }

  def toUiProducts(products: Iterable[(persist.Product, Iterable[persist.ProductName])],
                   fallback: String => String)(implicit userLocales: Array[java.util.Locale]): Iterable[Product] =
    products map {
      case (p, names) => toUiProduct(p, names, fallback)
    }
}

case class CatalogUiController(categoriesDao: persist.CategoriesDao = persist.CategoriesDao,
                               productsDao: persist.ProductsDao = persist.ProductsDao)
  extends CommonUiController("catalog") with CatalogUiSupport {

  get("/") {
    implicit val usrLocales = userLocales
    layoutTemplate(view("index"),
      TITLE -> messages("homepage"),
      "rootCategories" ->
        toUiCategories(categoriesDao.findRootCategories, categoryNameFallback))
  }

  get("/p:productId") {
    productsDao.findByIdWithName(params("productId")) match {
      case Some((product, productNames)) =>
        implicit val usrLocales = userLocales
        val uiProduct = toUiProduct(product, productNames, productNameFallback)
        val uiCategory: Option[Category] = uiProduct.categoryId match {
          case Some(categoryId) =>
            categoriesDao.findByIdWithName(categoryId) match {
              case Some((category, categoryNames)) =>
                Some(toUiCategory(category, categoryNames, categoryNameFallback))
              case None => ??? // FIXME specified missing product category
            }
          case None => None
        }
        layoutTemplate(view("product"),
          TITLE -> uiProduct.name,
          "parentCategory" -> uiCategory,
          "product" -> uiProduct)
      case None => resourceNotFound()
    }
  }

  get("/c:categoryId") {
    categoriesDao.findByIdWithName(java.lang.Long.parseLong(params("categoryId"))) match {
      case Some((category, categoryNames)) =>
        implicit val usrLocales = userLocales
        val uiCategory = toUiCategory(category, categoryNames, categoryNameFallback)
        val uiParentCategory: Option[Category] = category.parentId match {
          case Some(parentId) =>
            categoriesDao.findByIdWithName(parentId) match {
              case Some((parentCategory, parentCategoryNames)) =>
                Some(toUiCategory(parentCategory, parentCategoryNames, categoryNameFallback))
              case None => ??? // FIXME specified missing parent category
            }
          case None => None
        }
        val uiSubCategories = categoriesDao.findSubCategories(category) map {
          c =>
            categoriesDao.findByIdWithName(c.id) match {
              case Some((subCategory, subCategoryNames)) =>
                toUiCategory(subCategory, subCategoryNames, categoryNameFallback)
              case None => ??? // FIXME specified missing sub category
            }
        }
        val uiProducts = categoriesDao.findProducts(category) map {
          p =>
            productsDao.findByIdWithName(p.id) match {
              case Some((p, names)) =>
                toUiProduct(p, names, productNameFallback)
              case None => ??? // FIXME specified missing product
            }
        }

        layoutTemplate(view("category"),
          TITLE -> uiCategory.name,
          "category" -> uiCategory,
          "parentCategory" -> uiParentCategory,
          "subCategories" -> uiSubCategories,
          "products" -> uiProducts)
      case None => resourceNotFound()
    }
  }
}

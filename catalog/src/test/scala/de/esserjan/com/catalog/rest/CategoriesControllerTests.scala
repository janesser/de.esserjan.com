package de.esserjan.com.catalog.rest

import de.esserjan.com.catalog.persist
import de.esserjan.com.catalog.rest.CategoriesController.Category
import de.esserjan.com.rest.IdGenerator
import de.esserjan.com.test.ControllerTest
import org.mockito.Matchers._
import org.mockito.Mockito._

class CategoriesControllerTests extends ControllerTest with IdGenerator {

  implicit val dao = mock[persist.CategoriesDao]
  implicit val productsDao = mock[persist.ProductsDao]

  addServlet(new CategoriesController(dao, productsDao), "/c", "categories")

  test("Get root categories") {
    when(dao.findRootCategories)
      .thenReturn(List((persist.Category(id()), List.empty[persist.CategoryName])))

    get("/c/") {
      status should equal(200)
    }
  }

  test("Get non-existing category") {
    when(dao.findByIdWithName(anyLong())).thenReturn(None)

    val categoryId = id()
    get(s"/c/$categoryId") {
      status should equal(404)
    }
  }

  test("Put a category") {
    val categoryId = id()
    val jsonCategory: String = json.write(Category(categoryId, Map()))
    put("/c/", jsonCategory.getBytes) {
      status should equal(200)
      verify(dao, atLeastOnce).saveWithNames(any[persist.Category], any[Iterable[persist.CategoryName]])
    }
  }

  test("Post sub-category to category") {
    val categoryId = id()
    val subCategoryId = id() + 1L

    val category = persist.Category(categoryId)
    val subCategory = persist.Category(subCategoryId)

    when(dao.findById(categoryId)).thenReturn(Some(category))
    when(dao.findById(subCategoryId)).thenReturn(Some(subCategory))

    post(s"/c/$categoryId/subCategories/$subCategoryId") {
      status should equal(200)
      verify(dao, atLeastOnce).associateSubCategory(category, subCategory)
    }
  }

  test("Post product from category") {
    val categoryId = id()
    val c = persist.Category(categoryId)
    val productId = id().toString
    val p = persist.Product(productId)

    when(dao.findById(categoryId)).thenReturn(Some(c))
    when(productsDao.findById(productId)).thenReturn(Some(p))

    post(s"/c/$categoryId/products/$productId") {
      status should equal(200)
      verify(dao, atLeastOnce()).associateProduct(c,p)
    }
  }
}

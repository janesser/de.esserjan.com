package de.esserjan.com.catalog.ui

import de.esserjan.com.catalog.persist
import org.mockito.Mockito._
import org.scalatest.fixture
import org.scalatest.mock.MockitoSugar
import org.scalatra.test.scalatest.ScalatraSuite

class CatalogUiControllerTests
  extends fixture.FlatSpec
  with ScalatraSuite
  with MockitoSugar {

  val categoriesDao = mock[persist.CategoriesDao]

  val productsDao = mock[persist.ProductsDao]

  case class FixtureParam(category:persist.Category,
                          categoryNames:Iterable[persist.CategoryName],
                           product:persist.Product,
                           productNames:Iterable[persist.ProductName])

  override def withFixture(test:OneArgTest) = {
    val category = mock[persist.Category]
    val categoryName = mock[persist.CategoryName]

    when(category.id).thenReturn(
      123L, // self
      123L, // parent
      123L  // sub
    )
    when(category.parentId).thenReturn(None)
    when(categoryName.name).thenReturn("mockedCategoryName")
    when(categoryName.languageId).thenReturn("mockedLanguageTag")

    val product = mock[persist.Product]
    when(product.id).thenReturn("abc")
    when(product.category).thenReturn(Some(category))

    val productName = mock[persist.ProductName]
    when(productName.name).thenReturn("mockedProductName")
    when(productName.languageId).thenReturn("mockedLanguageTag")

    super.withFixture(test.toNoArgTest(FixtureParam(category, List(categoryName), product, List(productName))))
  }

  addServlet(new CatalogUiController(categoriesDao, productsDao), "/ui")

  val ACCEPT_LANGUAGE_HEADER =
    Map("Accept-Language" -> java.util.Locale.GERMANY.toLanguageTag)

  "CatalogUiController" should "render properly with no root category" in { t =>
    when(categoriesDao.findRootCategories).thenReturn(List())
    get("/ui", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "render properly with at least one root category" in { t =>
    when(categoriesDao.findRootCategories)
      .thenReturn(List((t.category, t.categoryNames)))
    get("/ui", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "render not found category" in { t =>
    when(categoriesDao.findByIdWithName(123L)).thenReturn(None)
    get("/ui/c123", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(404)
    }
  }

  it should "render category page without sub-categories and without products" in { t =>
    when(categoriesDao.findByIdWithName(123L))
      .thenReturn(Some((t.category, t.categoryNames)))
    when(categoriesDao.findSubCategories(t.category))
      .thenReturn(List())
    when(categoriesDao.findProducts(t.category))
      .thenReturn(List())


    get("/ui/c123", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "render category page with sub-category and with product" in { t =>
    when(t.category.parentId).thenReturn(Some(234L))

    when(categoriesDao.findByIdWithName(123L))
      .thenReturn(
        Some((t.category, t.categoryNames)), // as self
        Some((t.category, t.categoryNames))) // as sub
    when(categoriesDao.findByIdWithName(234L))
      .thenReturn(Some((t.category, t.categoryNames)))
    when(categoriesDao.findSubCategories(t.category))
      .thenReturn(List(t.category))
    /*when(categoriesDao.findByIdWithName(123L))
      .thenReturn(Some((t.category, t.categoryNames)))*/
    when(categoriesDao.findProducts(t.category))
      .thenReturn(List(t.product))
    when(productsDao.findByIdWithName("abc"))
      .thenReturn(Some((t.product, t.productNames)))

    get("/ui/c123", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }

  it should "render not found product" in { t =>
    when(productsDao.findByIdWithName("UnknownProductId")).thenReturn(None)
    get("/ui/pUnknownProductId", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(404)
    }
  }

  it should "render a product" in { t =>
    when(productsDao.findByIdWithName("KnownProductId"))
      .thenReturn(Some((t.product, t.productNames)))
    when(categoriesDao.findByIdWithName(123L))
      .thenReturn(Some((t.category, t.categoryNames)))

    get("/ui/pKnownProductId", headers = ACCEPT_LANGUAGE_HEADER) {
      status should equal(200)
    }
  }
}

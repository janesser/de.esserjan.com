package de.esserjan.com.catalog.rest

import de.esserjan.com.catalog.persist
import de.esserjan.com.catalog.rest.ProductsController.Product
import de.esserjan.com.test.ControllerTest
import org.mockito.Matchers._
import org.mockito.Mockito._

class ProductsControllerTests extends ControllerTest {

  import de.esserjan.com.catalog.rest.ProductsController.id

  implicit val dao = mock[persist.ProductsDao]

  addServlet(new ProductsController(dao), "/products", "products")

  test("save a product with name") {
    val jsonProduct = json.write(new Product(id(), Map("de_DE" -> "testProduct")))
    println(jsonProduct)
    put("/products", jsonProduct.getBytes) {
      status should equal(200)
    }
  }

  test("save a product without name") {
    val jsonProduct = json.write(new Product(id(), Map()))
    // println(jsonProduct)
    put("/products", jsonProduct.getBytes) {
      status should equal(200)
    }
  }

  test("find an existing product") {
    val pid = id()
    val product = persist.Product(pid)
    val names = List.empty[persist.ProductName]

    when(dao.findByIdWithName(anyString())).thenReturn(Some(product, names))

    get(s"/products/$pid") {
      status should equal(200)
    }
  }

  test("seek a non-existing product") {
    val pid = id()
    when(dao.findByIdWithName(anyString())).thenReturn(None)
    get(s"/products/$pid") {
      status should equal(404)
    }
  }

  test("delete an existing product") {
    val pid = id()
    when(dao.delete(anyString())).thenReturn(true)
    delete(s"/products/$pid") {
      status should equal(200)
    }
  }

  test("delete a non-existing product") {
    val pid = id()
    when(dao.delete(anyString())).thenReturn(false)
    delete(s"/products/$pid") {
      status should equal(404)
    }
  }
}

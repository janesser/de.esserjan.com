package de.esserjan.com.catalog.persist

import de.esserjan.com.persist.WithDatabaseTest
import org.scalatest.{FlatSpec, Matchers}
import org.squeryl.PrimitiveTypeMode._

class CatalogSchemaTests extends FlatSpec with Matchers with WithDatabaseTest[CatalogSchema.type] {

  override val schema = CatalogSchema

  "CatalogDbSchema" should "save altered product name" in withDatabase {
    s =>
      val pid = stringId()

      val p = Product(pid)
      val l = Language("de")
      val pn = ProductName(pid, l.id, "Awesome thingy.")

      s.products.insert(p)
      s.languages.insert(l)
      s.productNames.insert(pn)

      val p2 = s.products.where(p => p.id === pid).single
      p2.names.head should be(pn)

      val pn2 = ProductName(pid, l.id, "Really awesome thingy.")
      s.productNames.update(pn2)
  }

  it should "save altered relations" in withDatabase {
    s =>
      val pid = stringId()

      val p1 = Product(pid)
      val p2 = Product(pid+"second")
      val l = Language("de")
      val pn = ProductName(p1.id, l.id, "Awesome thingy.")

      s.products.insert(p1)
      s.products.insert(p2)
      s.languages.insert(l)
      s.productNames.insert(pn)

      pn.product.assign(p2) should be(p1)
  }
}

package de.esserjan.com.catalog.persist

import de.esserjan.com.persist.WithDatabaseTest
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ProductsDaoTests extends FlatSpec with Matchers with WithDatabaseTest[CatalogSchema.type] {

  override val schema =
    de.esserjan.com.catalog.persist.schema

  val dao = ProductsDao

  "ProductsDao" should "save, find and delete a product" in withDatabase { s =>
    val p = Product(stringId())
    dao.save(p)
    dao.findById(p.id) should equal(Some(p))
    dao.delete(p.id) should equal(true)
  }

  it should "save, find and delete two products with names" in withDatabase {
    s =>
      LanguagesDao.save(Language("de_DE"))
      LanguagesDao.save(Language("en_US"))

      val p1 = Product(stringId())
      val names1 = List(ProductName(p1.id, "de_DE", "Bier Dose"), ProductName(p1.id, "en_US", "beer can"))
      dao.saveWithNames(p1, names1)
      dao.saveWithNames(p1, names1) // save twice

      val p2 = Product(stringId())
      val names2 = List(ProductName(p2.id, "de_DE", "Bier Dose"), ProductName(p2.id, "en_US", "beer can"))
      dao.saveWithNames(p2, names2) // overlap in language

      dao.findByIdWithName(p1.id) match {
        case Some((p, names)) =>
          p should be(p)
          names.toList.size should equal(2)
        case None => fail("product should have been found")
      }
      dao.delete(p1.id) should equal(true)

      import org.squeryl.PrimitiveTypeMode._
      s.productNames.where(n => n.entityId === p1.id).headOption should be(None)
  }


  it should "not find nor delete a non-existing product" in withDatabase { s =>
    val pid = stringId()
    dao.findById(pid) should be(None)
    dao.delete(pid) should equal(false)
  }
}

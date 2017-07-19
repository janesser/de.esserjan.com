package de.esserjan.com.catalog.persist

import de.esserjan.com.catalog.persist.Product.IdType
import de.esserjan.com.persist._
import de.esserjan.com.{catalog, common}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl._

case class ProductName(override val entityId: Product.IdType,
                       override val languageId: Language.IdType,
                       override val name: String) extends PersistedEntityName[Product.IdType] {
  import de.esserjan.com.catalog.persist.schema._

  override def id = compositeKey(entityId, languageId)

  def language: ManyToOne[Language] =
    inTransaction {
      language2productNames.right(this)
    }

  def product: ManyToOne[Product] =
    inTransaction {
      product2productNames.right(this)
    }
}

object Product {
  type IdType = catalog.IdType
}

case class Product(override val id: Product.IdType)
  extends catalog.Product
  with NamedEntity[Product.IdType, ProductName] {

  import de.esserjan.com.catalog.persist.schema._

  def product = this

  override def name(lang: common.Language): Option[String] =
    inTransaction {
      val l = Language.languageForLocale(lang)
      val i = names.where(n => n.languageId === l.id).iterator
      if (i.hasNext) Some(i.next().name)
      else None
    }

  def names: OneToMany[ProductName] =
    inTransaction {
      product2productNames.left(this)
    }
}

trait ProductsDao extends NamedDao[Product.IdType, Product, ProductName]

object ProductsDao extends ProductsDao with PersistedNamedDao[Product.IdType, Product, ProductName] {
  override val table = schema.products

  override val namesTable = schema.productNames

  override def findName(id: CompositeKey2[Product.IdType, CommonLanguage.IdType]): Option[ProductName] =
    NamedDao.findNameWithStringRef[ProductName](id)(namesTable)

  override def findById(id: IdType): Option[Product] =
    Dao.findByStringId(id)(table)
}

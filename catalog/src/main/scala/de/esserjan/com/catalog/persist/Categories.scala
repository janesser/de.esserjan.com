package de.esserjan.com.catalog.persist

import de.esserjan.com.persist.CommonLanguage.IdType
import de.esserjan.com.persist._
import de.esserjan.com.{catalog, common}
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.{CompositeKey2, ManyToMany, ManyToOne, OneToMany}

case class CategoryProductRel(categoryId: Long, productId: catalog.IdType) extends KeyedEntity[CompositeKey2[Long, catalog.IdType]] {
  override def id = compositeKey(categoryId, productId)
}

case class CategoryName(override val entityId: Category.IdType,
                        override val languageId: String,
                        override val name: String)
  extends PersistedEntityName[Long] {

  import de.esserjan.com.catalog.persist.schema._

  override def id = compositeKey(entityId, languageId)

  override def language: ManyToOne[Language] =
    inTransaction {
      language2categoryNames.right(this)
    }

  def category: ManyToOne[Category] =
    inTransaction {
      category2categoryNames.right(this)
    }
}

object Category {
  type IdType = Long
}

case class Category(override val id: Category.IdType,
                    parentId: Option[Long /*Category.IdType*/ ] = None)
  extends NamedEntity[Category.IdType, CategoryName]
  with catalog.Category {

  import de.esserjan.com.catalog.persist.schema._

  override def name(lang: common.Language): Option[String] =
    inTransaction {
      val l = Language.languageForLocale(lang)
      val i = names.where(n => n.languageId === l.id).iterator
      if (i.hasNext) Some(i.next().name)
      else None
    }

  override def names: OneToMany[CategoryName] = {
    inTransaction {
      category2categoryNames.left(this)
    }
  }

  override def subCategories: OneToMany[Category] =
    inTransaction {
      category2subCategories.left(this)
    }

  override def products: ManyToMany[Product, CategoryProductRel] =
    inTransaction {
      categories2products.left(this)
    }
}

trait CategoriesDao extends NamedDao[Long, Category, CategoryName] {

  import de.esserjan.com.catalog.persist.schema._

  def findRootCategories: Iterable[(Category, Iterable[CategoryName])] =
    inTransaction {
      import scala.language.postfixOps
      categories.where(c => c.parentId isNull) map {
        c =>
          (c, c.names.toList)
      } toList
    }

  def findSubCategories(c: Category): List[Category] = {
    inTransaction {
      c.subCategories.toList
    }
  }

  def associateSubCategory(c: Category, subCat: Category): Unit =
    inTransaction {
      c.subCategories.associate(subCat)
    }

  def disassociateSubCategory(c: Category, subCat: Category): Unit

  def findProducts(c: Category): List[Product] = {
    inTransaction {
      c.products.toList
    }
  }

  def associateProduct(c: Category, p: Product): Unit = {
    inTransaction {
      c.products.associate(p)
    }
  }

  def dissociateProduct(c: Category, p: Product): Unit = {
    inTransaction {
      c.products.dissociate(p)
    }
  }
}

object CategoriesDao extends CategoriesDao with PersistedNamedDao[Long, Category, CategoryName] {
  override val table = schema.categories

  override val namesTable = schema.categoryNames

  def disassociateSubCategory(c: Category, subCat: Category): Unit =
    inTransaction {
      save(Category(subCat.id, /* parentId */ None))
    }

  override def findName(id: CompositeKey2[Long, IdType]): Option[CategoryName] =
    NamedDao.findNameWithLongRef[CategoryName](id)(namesTable)

  override def findById(id: Long): Option[Category] =
    Dao.findByLongId[Category](id)(table)
}
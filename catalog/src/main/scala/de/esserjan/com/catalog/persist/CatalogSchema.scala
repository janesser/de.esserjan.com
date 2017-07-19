package de.esserjan.com.catalog.persist

object CatalogSchema extends CatalogSchema

trait CatalogSchema extends org.squeryl.Schema {

  import org.squeryl.PrimitiveTypeMode._

  import scala.language.postfixOps

  val languages = table[Language]
  on(languages)(l => declare(
    l.id is primaryKey
  ))

  val products = table[Product]
  on(products)(p => declare(
    p.id is primaryKey
  ))


  val productNames = table[ProductName]

  val product2productNames = oneToManyRelation(products, productNames) via {
    (p, n) => p.id === n.entityId
  }
  product2productNames.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val language2productNames = oneToManyRelation(languages, productNames) via {
    (l, n) => l.id === n.languageId
  }

  val categories = table[Category]
  on(categories)(c => declare(
    c.id is primaryKey
  ))

  val categoryNames = table[CategoryName]

  val category2categoryNames = oneToManyRelation(categories, categoryNames) via {
    (c, n) => c.id === n.entityId
  }
  category2categoryNames.foreignKeyDeclaration.constrainReference(onDelete cascade)

  val language2categoryNames = oneToManyRelation(languages, categoryNames) via {
    (l, n) => l.id === n.languageId
  }

  val category2subCategories = oneToManyRelation(categories, categories) via {
    (c1, c2) =>
      c1.id === c2.parentId
  }

  val categories2products = manyToManyRelation(categories, products) via {
    (c, p, cp: CategoryProductRel) =>
      (cp.categoryId === c.id, cp.productId === p.id)
  }
}
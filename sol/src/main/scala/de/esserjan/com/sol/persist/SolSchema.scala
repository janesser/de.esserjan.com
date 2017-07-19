package de.esserjan.com.sol.persist

import de.esserjan.com.catalog

object SolSchema extends SolSchema
trait SolSchema extends org.squeryl.Schema {
  import scala.language.postfixOps
  import org.squeryl.PrimitiveTypeMode._
  import catalog.persist.CatalogSchema._

  val cartEntries = table[CartEntry]

  val product2cartEntries = oneToManyRelation(products, cartEntries) via {
    (p, e) => p.id === e.productId
  }

  val carts = table[Cart]

  val cart2cartEntries = oneToManyRelation(carts, cartEntries) via {
    (c,e) => c.id === e.cartId
  }
  cart2cartEntries.foreignKeyDeclaration.constrainReference(onDelete cascade)
}

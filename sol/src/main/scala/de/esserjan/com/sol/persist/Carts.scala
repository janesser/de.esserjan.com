package de.esserjan.com.sol.persist

import de.esserjan.com.catalog.persist
import de.esserjan.com.persist.Dao
import de.esserjan.com.{catalog, sol}
import org.squeryl.KeyedEntity
import org.squeryl.dsl.{OneToMany, ManyToOne}
import org.squeryl.PrimitiveTypeMode._

case class CartEntry(override val id: Long,
                     cartId: Long,
                     productId: catalog.IdType,
                     amountValue: BigDecimal,
                     amountType: String) extends KeyedEntity[Long] {
  import schema._

  def p: ManyToOne[persist.Product] =
    inTransaction {
      product2cartEntries.right(this)
    }

  def amount: sol.Amount =
    inTransaction {
      amountType match {
        case sol.Amount.PIECES =>
          sol.Pieces(amountValue.intValue())
        case sol.Amount.VOLUMES =>
          sol.Volume(amountValue)
        case _ => throw new IllegalArgumentException(s"unknown amountType: $amountType")
      }
    }
}

case class Cart(override val id: Long)
  extends sol.Cart
  with KeyedEntity[Long] {
  import schema._

  def entries: OneToMany[CartEntry] =
    inTransaction {
      cart2cartEntries.left(this)
    }

  override def amount(p: catalog.Product): Option[sol.Amount] =
    inTransaction {
      val i = entries.where(e => e.productId === p.id).iterator
      if (i.hasNext) Some(i.next().amount)
      else None
    }

  override def iterator: Iterator[catalog.Product] = {
    import scala.language.postfixOps
    inTransaction {
      entries map {
        e => e.p.single
      } toIterator
    }
  }

}

trait CartsDao extends Dao[Long, Cart] {
  def associateEntry(cart: Cart, entry: CartEntry): Unit
}

object CartsDao extends CartsDao {
  import schema._

  override def findById(cartId: Long): Option[Cart] =
    inTransaction {
      val i = carts.where(c => c.id === cartId).iterator
      if (i.hasNext) Some(i.next())
      else None
    }

  override def delete(cartId: Long): Boolean =
    inTransaction {
      carts.delete(cartId)
    }

  override def save(c: Cart): Unit =
    inTransaction {
      carts.insertOrUpdate(c)
    }

  override def associateEntry(cart: Cart, entry: CartEntry): Unit =
    inTransaction {
          cart.entries.associate(entry)
    }
}

trait CartEntriesDao extends Dao[Long, CartEntry]

object CartEntriesDao extends CartEntriesDao {
  import schema._

  override def findById(entryId: Long): Option[CartEntry] =
    inTransaction{
      val i = cartEntries.where(c => c.id === entryId).iterator
      if (i.hasNext) Some(i.next())
      else None
    }


  override def delete(entryId: Long): Boolean =
    inTransaction {
      cartEntries.delete(entryId)
    }

  override def save(e: CartEntry): Unit =
    inTransaction {
      cartEntries.insertOrUpdate(e)
    }
}
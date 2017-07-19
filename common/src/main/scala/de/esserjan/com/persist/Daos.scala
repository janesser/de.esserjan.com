package de.esserjan.com.persist

import org.squeryl.{Table, KeyedEntity}
import org.squeryl.PrimitiveTypeMode._

object Dao {
  def findByStringId[V <: KeyedEntity[String]](id: String)(implicit table: Table[V]): Option[V] =
    inTransaction {
      table.where(v => v.id === id).headOption
    }

  def findByLongId[V <: KeyedEntity[Long]](id: Long)(implicit table: Table[V]): Option[V] =
    inTransaction {
      table.where(v => v.id === id).headOption
    }
}

trait Dao[K, V <: KeyedEntity[K]] {
  def findById(id: K): Option[V]

  def save(v: V): Unit

  def delete(id: K): Boolean
}

trait GenericDao[K, V <: KeyedEntity[K]] extends Dao[K, V] {

  def table: org.squeryl.Table[V]

  override def delete(id: K): Boolean =
    inTransaction {
      table.delete(id)
    }

  override def save(v: V): Unit =
    inTransaction {
      findById(v.id) match {
        case Some(persisted) =>
          if (persisted != v)
            table.update(v)
        case None =>
          table.insert(v)
      }
    }
}

case class DaoReference(_dao: AnyRef) {
  def dao[T <: Dao[_,_]]: T = _dao.asInstanceOf[T]
}
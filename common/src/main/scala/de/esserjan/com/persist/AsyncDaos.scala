package de.esserjan.com.persist

import gnieh.sohva.IdRev
import gnieh.sohva.async.Database
import org.squeryl.KeyedEntity

import scala.concurrent.Future

trait AsyncDao[K, V <: KeyedEntity[K]] {
  def getDocById(id: K): Future[Option[V]]

  def deleteDoc(id: K): Future[Boolean]

  def saveDoc(doc: V): Future[V]
}

class GenericAsyncDao[V <: IdRev with KeyedEntity[String]](db: Database)(implicit m: Manifest[V]) extends AsyncDao[String, V] {
  override def getDocById(id: String): Future[Option[V]] =
    db.getDocById[V](id)

  override def deleteDoc(id: String): Future[Boolean] =
    db.deleteDoc(id)

  override def saveDoc(doc: V): Future[V] =
    db.saveDoc(doc)
}

trait Synced {
  import scala.concurrent.Await
  import scala.concurrent.duration._

  val atMost = 5.seconds

  def synced[T](future:Future[T]):T = {
    Await.result[T](future, atMost)
  }
}

trait GenericSyncDao[V <: IdRev with KeyedEntity[String]] extends Dao[String, V] with Synced {
  self: GenericAsyncDao[V] =>

  override def findById(id: String): Option[V] =
    synced(self.getDocById(id))

  override def delete(id: String): Boolean =
    synced(self.deleteDoc(id))

  override def save(doc: V): Unit =
    synced(self.saveDoc(doc))
}
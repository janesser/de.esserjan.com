package de.esserjan.com.persist

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.dsl.{ManyToOne, CompositeKey2, OneToMany}
import org.squeryl.{KeyedEntity, Table}

trait EntityName[K] extends KeyedEntity[CompositeKey2[K, String]] {
  type L <: CommonLanguage

  def entityId: K

  def languageId: CommonLanguage.IdType

  def id = compositeKey(entityId, languageId)

  def name: String

  def language: ManyToOne[L]
}

trait NamedEntity[K, N <: EntityName[K]] extends KeyedEntity[K] {
  def names: OneToMany[N]
}

object NamedDao {
  def findNameWithStringRef[N <: EntityName[String]](id: CompositeKey2[String, CommonLanguage.IdType])(implicit namesTable: Table[N]): Option[N] =
    inTransaction {
      namesTable.where(n => n.entityId === id.a1 and n.languageId === id.a2).headOption
    }

  def findNameWithLongRef[N <: EntityName[Long]](id: CompositeKey2[Long, CommonLanguage.IdType])(implicit namesTable: Table[N]): Option[N] =
    inTransaction {
      namesTable.where(n => n.entityId === id.a1 and n.languageId === id.a2).headOption
    }
}

trait NamedDao[K, V <: NamedEntity[K, N], N <: EntityName[K]] extends GenericDao[K, V] {

  import org.squeryl.PrimitiveTypeMode._

  def namesTable: Table[N]

  def saveLanguage(langId:CommonLanguage.IdType)

  def findByIdWithName(id: K): Option[(V, Iterable[N])] =
    inTransaction {
      findById(id) match {
        case Some(entity) =>
          Some((entity, entity.names))
        case None =>
          None
      }
    }

  def findName(id: CompositeKey2[K, CommonLanguage.IdType]): Option[N]

  def saveWithNames(v: V, names: Iterable[N]): Unit = {
    inTransaction {
      save(v)
      names foreach {
        n =>
          saveLanguage(n.languageId)
          saveName(n)
      }
    }
  }

  def saveName(n: N): Unit = {
    inTransaction {
      findName(n.id) match {
        case Some(persisted) =>
          if (persisted != n)
            namesTable.update(n)
        case None =>
          namesTable.insert(n)
      }
    }
  }
}
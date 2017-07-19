package de.esserjan.com.catalog

import de.esserjan.com.persist.{EntityName, NamedDao, NamedEntity}

package object persist {

  val schema = CatalogSchema

  trait PersistedEntityName[K] extends EntityName[K] {
    override type L = Language
  }

  trait PersistedNamedDao[K, V <: NamedEntity[K, N], N <: EntityName[K]] extends NamedDao[K, V, N] {
    override def saveLanguage(langId: IdType): Unit =
      LanguagesDao.findById(langId) match {
        case Some(l) =>
          // ignore
        case None =>
          LanguagesDao.save(Language(langId))
      }
  }

}

package de.esserjan.com.catalog.persist

import de.esserjan.com.persist.{CommonLanguage, Dao, GenericDao}

object Language {
  type IdType = CommonLanguage.IdType

  def languageForLocale(locale: java.util.Locale):Language =
    Language(locale.toLanguageTag)

  def languageForInput(langTag: String): Language =
    languageForLocale(java.util.Locale.forLanguageTag(langTag))
}

case class Language(override val id: Language.IdType)
  extends CommonLanguage

trait LanguagesDao extends Dao[Language.IdType, Language]

object LanguagesDao extends LanguagesDao with GenericDao[Language.IdType, Language] {
  override val table = schema.languages

  override def findById(id: Language.IdType): Option[Language] =
    Dao.findByStringId[Language](id)(table)
}

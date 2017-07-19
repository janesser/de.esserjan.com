package de.esserjan.com.persist

import org.squeryl.KeyedEntity
import org.squeryl.annotations.Transient

object CommonLanguage {
  type IdType = String

  def languageForLocale(loc:java.util.Locale) =
    new CommonLanguage {
      val id = loc.toLanguageTag
    }

  def languageForInput(languageId:String) =
    languageForLocale(java.util.Locale.forLanguageTag(languageId))
}

trait CommonLanguage extends KeyedEntity[CommonLanguage.IdType] {
  @Transient
  def toLocale = java.util.Locale.forLanguageTag(id)

  @Transient
  override def equals(that:Any):Boolean =
    if (that.isInstanceOf[CommonLanguage])
      that.asInstanceOf[CommonLanguage].id == id
  else false
}

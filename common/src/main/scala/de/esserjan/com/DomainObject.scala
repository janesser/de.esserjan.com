package de.esserjan.com

package object common {
  type Language = java.util.Locale
}

trait Addressable[ID_TYPE] {
  def id: ID_TYPE
}

trait Named {
  def name(loc: common.Language): Option[String]
}

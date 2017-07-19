package de.esserjan.com.catalog

import de.esserjan.com.{Named, Addressable}

trait Product extends Addressable[IdType] with Named {
  def category:Option[Category] = None
}

trait Category extends Addressable[Long] with Named {
  def subCategories:Iterable[Category]

  def products:Iterable[Product]
}
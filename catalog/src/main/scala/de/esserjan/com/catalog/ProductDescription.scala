package de.esserjan.com.catalog

import de.esserjan.com.common

trait ProductDescriptionDocument {
  def language: common.Language

  def productId: IdType

  def items: Map[String, DescriptionItem]
}

sealed trait DescriptionItem {
  def descriptionType: DescriptionType = DescriptionTypes.ONLINE
}

trait MediaItem extends DescriptionItem {
  def mediaType: MediaType

  def retrieve(): Binary

  def reference: java.net.URL
}

trait TextualItem extends DescriptionItem {
  def sectionId: String

  def text: Text
}

trait AttributesItem extends DescriptionItem {
  def attributes:Map[String, Either[AnyVal, Seq[AnyVal]]]
}
package de.esserjan.com

package object catalog {
  type IdType = String

  type Text = String
  type Binary = scala.io.Source

  object DescriptionTypes extends Enumeration {
    type DescriptionTypes = Value
    val ONLINE = Value
    val PRINT = Value
  }

  type DescriptionType = DescriptionTypes.DescriptionTypes

  object MediaTypes extends Enumeration {
    type MediaTypes = Value
    val IMAGE = Value
    val DOCUMENT = Value

    def mediaTypeForContentType(contentType:String):MediaType = {
      if (contentType.startsWith("images/")) IMAGE
      else DOCUMENT
    }
  }

  type MediaType = MediaTypes.MediaTypes

}

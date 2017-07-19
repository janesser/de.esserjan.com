package de.esserjan.com.catalog.persist

import java.io.InputStream
import java.net.URL

import de.esserjan.com.catalog._
import de.esserjan.com.persist._
import de.esserjan.com.{common, catalog}
import gnieh.sohva.{IdRev, Attachments}
import gnieh.sohva.async.Database
import org.squeryl.KeyedEntity

object ProductDescriptionDocument {
  type IdType = String

  def documentId(productId: persist.Product.IdType, languageId: persist.Language.IdType): String =
    s"$productId^$languageId"

  def attachId(dt: DescriptionType, mt: Option[catalog.MediaType] = None): String = {
    val descType = dt.toString
    val mediaType = mt.toString
    s"$descType^$mediaType"
  }

  def apply(p: catalog.Product, l: common.Language): ProductDescriptionDocument =
    new ProductDescriptionDocument(p.id, CommonLanguage.languageForLocale(l).id)
}

case class ProductDescriptionDocument(productId: String,
                                      languageId: String,
                                      @transient items: Map[String, DescriptionItem] = Map())
  extends catalog.ProductDescriptionDocument
  with KeyedEntity[String]
  with IdRev
  with Attachments {

  override val _id = id

  override def id =
    ProductDescriptionDocument.documentId(
      productId,
      CommonLanguage.languageForLocale(language).id)

  override def language: common.Language =
    CommonLanguage.languageForInput(languageId).toLocale
}


trait ProductDescriptionsDao
  extends Dao[ProductDescriptionDocument.IdType, ProductDescriptionDocument] {

  def db: Database

  def saveAttachment(docId: String,
                     attachId: String,
                     stream: InputStream,
                     contentType: String): Boolean

  def getAttachment(docId: String,
                    attachId: String): Option[(String, InputStream)]

  def getAttachments(docId: String): Map[String, DescriptionItem]


  def deleteAttachment(docId: String,
                        attachId: String): Boolean
}

class ProductDescriptionsDaoImpl(override val db: Database)
  extends GenericAsyncDao[ProductDescriptionDocument](db)
  with GenericSyncDao[ProductDescriptionDocument]
  with ProductDescriptionsDao {

  override def getAttachments(docId: String): Map[String, DescriptionItem] =
    findById(docId) match {
      case Some(pd) =>
        import scala.language.postfixOps
        pd._attachments map {
          case (attachmentId, attachment) =>
            attachmentId -> {
              attachment.content_type match {
                case "text/plain" =>
                  new catalog.TextualItem {
                    private lazy val content =
                      synced(db.getAttachment(docId, attachmentId)) match {
                        case Some((contentType, inputStream)) =>
                          scala.io.Source.fromInputStream(inputStream).getLines().toSeq
                        case None =>
                          throw new IllegalStateException("attachment deleted")
                      }

                    override def sectionId: String = content.head

                    override def text: Text = content.tail.reduce((s1, s2) => s"$s1\n$s2")
                  }
                case someContentType: String =>
                  new catalog.MediaItem {
                    override def mediaType: MediaType =
                      MediaTypes.mediaTypeForContentType(someContentType)

                    // forward declaration to be implemented in UI layer
                    override def reference: URL =
                      throw new NotImplementedError()

                    override def retrieve(): Binary = synced(db.getAttachment(docId, attachmentId)) match {
                      case Some((contentType, inputStream)) =>
                        scala.io.Source.fromInputStream(inputStream)
                      case None =>
                        throw new IllegalStateException("attachment deleted")
                    }
                  }
              }
            }
        } toMap
      case None => Map() // no attachments
    }

  override def saveAttachment(docId: String, attachId: String, stream: InputStream, contentType: String): Boolean =
    synced(db.attachTo(docId, attachId, stream, contentType))

  override def getAttachment(docId: String, attachId: String): Option[(String, InputStream)] =
    synced(db.getAttachment(docId, attachId))

  override def deleteAttachment(docId: String, attachId: String): Boolean =
    synced(db.deleteAttachment(docId, attachId))
}
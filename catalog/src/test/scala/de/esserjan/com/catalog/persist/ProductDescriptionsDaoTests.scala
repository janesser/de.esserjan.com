package de.esserjan.com.catalog.persist

import java.io._
import java.util.concurrent.TimeoutException

import akka.actor.ActorSystem
import akka.util.Timeout
import de.esserjan.com.catalog
import de.esserjan.com.catalog.TextualItem
import gnieh.sohva.async.CouchClient
import gnieh.sohva.{Attachment, Attachments, IdRev, async}
import net.liftweb.json._
import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.io.Source

object ProductDescriptionsDaoTests {
  val PRODUCT_ID = "mockedProductDescriptionId"

  val LANGUAGE_ID = "de-DE"

  val DOCUMENT_ID = s"$PRODUCT_ID^$LANGUAGE_ID"

  val ATTACHMENT_ID = "mockedAttachmentId"
}

@RunWith(classOf[JUnitRunner])
class ProductDescriptionsDaoTests
  extends FlatSpec
  with Matchers
  with MockitoSugar {

  import de.esserjan.com.catalog.persist.ProductDescriptionsDaoTests._

  val asyncDb = mock[async.Database]

  val product = mock[catalog.Product]
  when(product.id).thenReturn(PRODUCT_ID)
  val language = java.util.Locale.forLanguageTag(LANGUAGE_ID)

  val dao = new ProductDescriptionsDaoImpl(asyncDb) {
    override val atMost = 1.second
  }

  "ProductDescriptionDocument" should "be serializable (write and read)" in {
    implicit val formats: Formats = DefaultFormats

    val desc = ProductDescriptionDocument(product, language)

    val writer = new StringWriter()
    Serialization.write(desc, writer)

    val reader = new StringReader(writer.toString)
    val desc2: ProductDescriptionDocument =
      Serialization.read[ProductDescriptionDocument](reader)
    desc2.id should equal(desc.id)
  }

  it should "be decomposable" in {
    implicit val formats: Formats = DefaultFormats +
      FieldSerializer[IdRev]() +
      FieldSerializer[Attachments]()

    val desc = ProductDescriptionDocument(product, language)

    Extraction.decompose(desc)
  }

  "ProductDescriptionsDao" should "find a description doc by ID" in {
    val desc = ProductDescriptionDocument(product, language)
    desc.id should equal(DOCUMENT_ID)

    when(asyncDb.getDocById[ProductDescriptionDocument](DOCUMENT_ID))
      .thenReturn(Future.successful(Some(desc)))
    dao.findById(DOCUMENT_ID) should be(Some(desc))

    // verify(syncDb, never()).getDocById[ProductDescriptionDocument](anyString())
  }

  it should "fail on Future failure" in {
    when(asyncDb.getDocById[ProductDescriptionDocument](DOCUMENT_ID))
      .thenReturn(Future.failed(new Exception("mockedException")))
    intercept[Exception] {
      dao.findById(DOCUMENT_ID)
    }
  }

  it should "timeout Future" in {
    when(asyncDb.getDocById[ProductDescriptionDocument](DOCUMENT_ID))
      .thenReturn(Promise().future)
    intercept[TimeoutException] {
      dao.findById(DOCUMENT_ID)
    }
  }

  it should "respond on non-existing document" in {
    when(asyncDb.getDocById[ProductDescriptionDocument](DOCUMENT_ID))
      .thenReturn(Future.successful(None))
    dao.findById(DOCUMENT_ID) should be(None)
  }

  it should "serve attachments" in {
    val desc = spy(ProductDescriptionDocument(product, language))
    desc.id should equal(DOCUMENT_ID)
    when(asyncDb.getDocById[ProductDescriptionDocument](DOCUMENT_ID))
      .thenReturn(Future.successful(Some(desc)))

    val attachment = Attachment(
      /*content_type*/ "text/plain",
      /*revpos*/ 0,
      /*digest*/ "mockedDigest",
      /*length*/ 0,
      /*stub */ true)
    doReturn(Map("mockedAttachId" -> attachment)).when(desc)._attachments

    val inputStream: InputStream = new ByteArrayInputStream("mockedAttachmentContent".getBytes("utf-8"))
    when(asyncDb.getAttachment(DOCUMENT_ID, "mockedAttachId"))
      .thenReturn(Future.successful(Some(("text/plain", inputStream))))

    val textualItem = dao.getAttachments(DOCUMENT_ID).head
    textualItem._2.asInstanceOf[TextualItem]
      .sectionId should not be empty
  }

  /**
   * @see SohvaTests
   */
  ignore should "round-trip with real CouchDb" in {
    implicit val system = ActorSystem("IntegrationTestCouchDbActors")
    implicit val timeout = Timeout(5.second)
    val DDL_TO = 5.seconds

    val client = new CouchClient()
    val db = client.database("testcouchdb1")

    val doc = ProductDescriptionDocument(product, language)

    try {
      val dao = new ProductDescriptionsDaoImpl(db)
      Await.ready(db.create, DDL_TO)

      val doc2 = Await.result(dao.saveDoc(doc), dao.atMost)
      dao.findById(doc2.id) match {
        case None => fail("no document after save")
        case Some(doc3) =>
          val attachmentStream = getClass.getResourceAsStream("/test_attachment.txt")
          dao.saveAttachment(DOCUMENT_ID, ATTACHMENT_ID, attachmentStream, "text/plain") should equal(true)
          dao.getAttachment(DOCUMENT_ID, ATTACHMENT_ID) match {
            case None => // FIXME sohva issue#43 // fail("no attachment after attach")
            case Some((contentType, inputStream)) =>
              Source.fromInputStream(inputStream).getLines().hasNext should equal(true)
              dao.deleteAttachment(DOCUMENT_ID, ATTACHMENT_ID) should equal(true)
              dao.delete(DOCUMENT_ID) should equal(true)
          }
          val attachment = dao.getAttachments(DOCUMENT_ID).head
          attachment._1 should equal(ATTACHMENT_ID)
          attachment._2 shouldBe a[TextualItem]
      }
    } finally {
      Await.ready(db.delete, DDL_TO)
      client.shutdown()
      system.shutdown()
    }
  }
}

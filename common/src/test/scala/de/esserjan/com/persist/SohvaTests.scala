package de.esserjan.com.persist

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import gnieh.sohva.sync.CouchClient
import gnieh.sohva.{Attachments, IdRev}
import org.scalatest.{FlatSpec, Ignore, Matchers}

object TestDocument {
  def apply(id: String, text: String, rev: Option[String] = None):TestDocument =
    TestDocument(id, text).withRev(rev)
}

case class TestDocument(override val _id: String, text: String) extends IdRev with Attachments

object SohvaTests {
  val DOC_ID = "123"
  val ATTACH_ID = "abc"
}

/**
 * Integration test with sohva as client-api to CouchDb.
 *
 * Requires CouchDb Server at localhost:5984 (no-login).
 */
@Ignore
class SohvaTests extends FlatSpec with Matchers {

  import de.esserjan.com.persist.SohvaTests._

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(10, TimeUnit.SECONDS)

  val couch = new CouchClient()

  "CouchClient" should "connect to server and create database" in {
    val db = couch.database("test1")
    db.create

    try {
      db.deleteDoc(DOC_ID)
      db.saveDoc(TestDocument(DOC_ID, "Some multi-line text\n\n...", None))
      val doc: Option[TestDocument] = db.getDocById[TestDocument](DOC_ID)
      doc match {
        case None => fail(s"could not get doc by ID $DOC_ID")
        case Some(doc2) =>
          val attach = Paths.get(getClass.getResource("/test_attachment.txt").toURI).toFile
          // TODO work with inputStreams
          db.attachTo(DOC_ID,
            attach,
            "text/plain") should equal(true)

          // fetch attachment by ID
          db.getAttachment(DOC_ID, "test_attachment.txt") match {
            case Some((contentType, stream)) =>
              println(s"$contentType, $stream")
              stream.close()
            case None => fail("No attachment found!")
          }

          // fetch attachment on document
          val saved = db.getDocById[TestDocument](DOC_ID).get
          val attachments = saved._attachments
          attachments.head._1 should equal("test_attachment.txt")
          println(s"$attachments")
      }
    } finally {
      db.delete
      couch.shutdown()
    }
  }
}
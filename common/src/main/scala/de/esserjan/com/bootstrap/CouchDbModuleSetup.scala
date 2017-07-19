package de.esserjan.com.bootstrap

import akka.actor.ActorSystem
import akka.util.Timeout
import gnieh.sohva.async.{CouchClient, Database}
import scala.concurrent.duration._

trait CouchDbModuleSetup extends ModuleSetup {
  implicit val timeout: Timeout = 5.seconds

  def couchClientSetup()(implicit system: ActorSystem): CouchClient =
    new CouchClient()

  def couchDatabases()(implicit client: CouchClient): Map[String, Database]

  override def couchSetup(createDatabase: Boolean)(implicit system: ActorSystem): Map[String, Database] = {
    implicit val client = couchClientSetup()
    couchDatabases()
  }
}

object CouchDbProps {
  val COUCHDB_HOST = "couchdb.host"
  val COUCHDB_PORT = "couchdb.port"
  val COUCHDB_SSL = "couchdb.ssl"
  val COUCHDB_VERSION = "couchdb.version"
}

trait CouchDbPropsModuleSetup extends CouchDbModuleSetup with PropertiesSupport {

  import de.esserjan.com.bootstrap.CouchDbProps._

  override def couchClientSetup()(implicit system: ActorSystem): CouchClient = {
    val host = props.getProperty(COUCHDB_HOST)
    val port = java.lang.Integer.parseInt(
      props.getProperty(COUCHDB_PORT))
    val ssl = java.lang.Boolean.parseBoolean(
      props.getProperty(COUCHDB_SSL))
    val version = props.getProperty(COUCHDB_VERSION)

    new gnieh.sohva.async.CouchClient(host, port, ssl, version)
  }
}
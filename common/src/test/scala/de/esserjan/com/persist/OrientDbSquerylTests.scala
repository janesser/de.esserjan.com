package de.esserjan.com.persist

import java.sql.{DriverManager, SQLException}
import java.util.Properties
import javax.persistence.Id

import com.orientechnologies.orient.`object`.db.OObjectDatabaseTx
import com.orientechnologies.orient.core.metadata.schema.{OClass, OType}
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.jdbc.{OrientJdbcConnection, OrientJdbcDriver}
import com.orientechnologies.orient.server._
import org.scalatest.{FlatSpec, Ignore, Matchers}
import org.squeryl._
import org.squeryl.internals.{DatabaseAdapter, StatementWriter}

import scala.collection.convert.{DecorateAsJava, DecorateAsScala, WrapAsJava, WrapAsScala}

@Ignore
class OrientDbSquerylTests extends FlatSpec with Matchers {

  object WithInMemoryOrientDb
    extends {}
    with DecorateAsJava
    with DecorateAsScala
    with WrapAsScala
    with WrapAsJava {

    val ORIENT_CONFIG =
      <orient-server>
        <network>
          <protocols>
            <protocol name="binary" implementation="com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary"/>
            <protocol name="http" implementation="com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb"/>
          </protocols>
          <listeners>
            <listener ip-address="0.0.0.0" port-range="2424-2430" protocol="binary"/>
            <listener ip-address="0.0.0.0" port-range="2480-2490" protocol="http"/>
          </listeners>
        </network>
      </orient-server>
  }

  case class WithInMemoryOrientDb(classes: Class[_]*)(testFun: OObjectDatabaseTx => Unit) {

    import WithInMemoryOrientDb._

    val server: OServer = OServerMain.create()
    server.startup(ORIENT_CONFIG.mkString)
    server.activate()
    val db: OObjectDatabaseTx = new OObjectDatabaseTx("memory:myDb")
    try {
      db.create()
      val em = db.getEntityManager()
      classes.foreach(em.registerEntityClass(_))
      testFun(db)
    } finally {
      db.drop()
      db.close()
      server.shutdown()
    }
  }

  "OrientDb" should "do round-trip in-memory schema-less" in
    WithInMemoryOrientDb() { db =>
      import WithInMemoryOrientDb._

      val doc = new ODocument()
      doc.field("name", "aName")

      doc.save()
      val rid = doc.getIdentity()
      rid should not be (null)

      val docs: java.util.List[ODocument] = db.query(new OSQLSynchQuery[ODocument]("select from " + rid))
      docs.asScala.toList.headOption match {
        case None => fail("nothing found")
        case Some(doc2) =>
          doc2 should equal(doc)
      }
    }

  it should "generate schema via javassist and synthesised getters" in
    WithInMemoryOrientDb(classOf[PersonBean]) { db =>
      val p = db.newInstance(classOf[PersonBean])
      p.name = "aName"
      val p2: PersonBean = db.save(p)
      p2.getId should not equal (null)
      p2.getName should equal("aName")

      db.getEntityManager.getRegisteredEntities.isEmpty should equal(false)
    }

  it should "generate schema from scala case-class" in
    WithInMemoryOrientDb() { db =>
      import scala.reflect.runtime.{universe => ru}

      val personOClass: OClass = db.getMetadata.getSchema.createClass(classOf[Person])

      val constructor = ru.typeOf[Person].decl(ru.termNames.CONSTRUCTOR).asMethod
      val m = ru.runtimeMirror(this.getClass.getClassLoader)

      constructor.paramLists foreach {
        paramGroup =>
          paramGroup foreach {
            param =>
              val paramName: String = param.name.toString
              val oType: OType = {
                val clazz: Class[_] = m.runtimeClass(param.typeSignature.typeSymbol.asClass)
                OType.getTypeByClass(clazz)
              }
              val prop = personOClass.createProperty(paramName, oType)

              param.annotations.find(_.tree.tpe =:= ru.typeOf[javax.persistence.Id]) match {
                case Some(_) =>
                  prop.createIndex(OClass.INDEX_TYPE.UNIQUE_HASH_INDEX)
                case None => // ignore
              }

              val indices = prop.getAllIndexes

              println(s"$prop $indices")
          }
      }

      personOClass.getIndexes.isEmpty should equal(false)
      personOClass.properties().isEmpty should equal(false)
    }

  ignore should "be wrapped by Squeryl DatabaseAdapter" in
    WithInMemoryOrientDb() { db =>
      import org.squeryl.PrimitiveTypeMode._

      DriverManager.registerDriver(new OrientJdbcDriver)

      val props = new Properties()
      val c = DriverManager.getConnection("jdbc:orient:remote:localhost/myDb", props).asInstanceOf[OrientJdbcConnection]

      SessionFactory.concreteFactory = Some(() => Session.create(c, new DatabaseAdapter {
        override def writeCreateTable[T](t: Table[T], sw: StatementWriter, schema: Schema) = {
          sw.write("CREATE CLASS ")
          sw.write(t.name)
        }

        override def isTableDoesNotExistException(e: SQLException): Boolean = false // schema-less
      }))

      val schema = new org.squeryl.Schema {
        val persons = table[Person]
      }

      transaction {
        // SQLFeatureNotSupportedException
        schema.create

        schema.persons.insert(Person(1L, "Jane Doe"))

        schema.persons.where(p => p.id === 1L)
      }
    }
}

class PersonBean {
  @Id var id: Long = _

  def getId = id

  var name: String = _

  def getName = name
}

case class Person(@Id override val id: Long, name: String) extends KeyedEntity[Long]
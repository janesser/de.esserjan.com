package de.esserjan.com.persist

import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{Schema, Session, SessionFactory}

trait WithDatabaseTest[S <: Schema] {

  private[this] val JDBC_URL = "jdbc:h2:mem:catalog"
  private[this] val JDBC_USER = "sa"
  private[this] val JDBC_PASS = ""

  def schema: S

  def id() = System.currentTimeMillis()

  def stringId() = id().toString

  def withDatabase(testCode: S => Any) = {
    val s = Session.create(
      java.sql.DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS),
      new org.squeryl.adapters.H2Adapter)

    SessionFactory.concreteFactory = Some(() => s)

    transaction {
      schema.drop
      schema.create

      testCode(schema)
    }
  }
}

package de.esserjan.com.bootstrap

trait SquerylModuleSetup extends ModuleSetup {

  import org.squeryl.PrimitiveTypeMode._
  import org.squeryl.Schema

  protected def schemas: Seq[Schema]

  override def afterSessionFactorySet(initializeSchema: Boolean): Unit =
    if (initializeSchema)
      transaction {
        schemas.foreach {
          s =>
            s.drop
            s.create
        }
      }
}
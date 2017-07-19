package de.esserjan.com.bootstrap

trait SquerylDatasourceSupport {

  import org.squeryl._

  protected def adapterClass: Class[_ /*internals.DatabaseAdapter*/ ]

  protected def dataSource: javax.sql.DataSource

  def prepareDataSource(): Unit = {
    SessionFactory.concreteFactory = Some(
      () => Session.create(
        dataSource.getConnection,
        adapterClass.newInstance.asInstanceOf[internals.DatabaseAdapter]
      ))
  }

  def shutdown(): Unit
}

trait HikariCpSupport extends SquerylDatasourceSupport with PropertiesSupport {

  import com.zaxxer.hikari._

  override def propsPath = "hikari.properties"

  override lazy val dataSource =
    new HikariDataSource(
      new HikariConfig(props))

  override def shutdown() = dataSource.close()
}

trait DbcpSupport extends SquerylDatasourceSupport {

  import org.apache.commons.dbcp2._

  override lazy val dataSource = {
    val props = new java.util.Properties
    props.load(getClass.getResourceAsStream("/dbcp.properties"))
    val cpds = new cpdsadapter.DriverAdapterCPDS
    cpds.setDriver(props.getProperty("dbcp.driver"))
    cpds.setUrl(props.getProperty("dbcp.url"))
    cpds.setUser(props.getProperty("dbcp.username"))
    cpds.setPassword(props.getProperty("dbcp.password"))

    val ds = new datasources.SharedPoolDataSource
    ds.setConnectionPoolDataSource(cpds)
    ds.setMaxTotal(
      java.lang.Integer.parseInt(
        props.getProperty("dbcp.maxTotal")))
    ds
  }

  override def shutdown() = dataSource.close()
}

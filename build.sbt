name := "de.esserjan.com"

version := "1.0"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-target:jvm-1.7", "-feature" /*, "-deprecated"*/)

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-runner" % "9.2.5.v20141112" % "container",
  "com.zaxxer" % "HikariCP-java6" % "2.2.5" % "container",
  "com.h2database" % "h2" % "1.4.182" % "container",
  "ch.qos.logback" % "logback-classic" % "1.1.1" % "runtime"
)

lazy val common = project

lazy val bootstrap = project.dependsOn(catalog, sol, common)

lazy val catalog = project.dependsOn(common % "test->test;compile->compile")

lazy val sol = project.dependsOn(catalog)

lazy val checkout = project.dependsOn(sol)

lazy val root = project
  .in(file("."))
  .aggregate(catalog, sol, common, bootstrap)
  .dependsOn(catalog, sol, common, bootstrap)

fork in Test := true

jetty(options = new ForkOptions(workingDirectory = Some(file("bootstrap/src/main/resources"))))

webappSrc in webapp <<= baseDirectory map {
  _ / "bootstrap/src/main/webapp"
}

webappSrc in container <<= baseDirectory map {
  _ / "bootstrap/src/main/webapp"
}

net.virtualvoid.sbt.graph.Plugin.graphSettings
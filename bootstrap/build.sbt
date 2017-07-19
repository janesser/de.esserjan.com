name := "bootstrap"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra_2.11" % "2.3.0",
  "org.scalatra" % "scalatra-swagger_2.11" % "2.3.0",
  "org.scalatra" % "scalatra-json_2.11" % "2.3.0",
  "org.scalatra" % "scalatra-scalate_2.11" % "2.3.0",
  "org.json4s" % "json4s-jackson_2.11" % "3.2.10",
  "org.json4s" % "json4s-native_2.11" % "3.2.10" % "provided",
  "org.squeryl" % "squeryl_2.11" % "0.9.5-7",
  "com.zaxxer" % "HikariCP-java6" % "2.2.5" % "provided",
  "com.h2database" % "h2" % "1.4.182" % "provided",
  "org.apache.commons" % "commons-dbcp2" % "2.0.1" % "provided",
  "org.apache.derby" % "derby" % "10.11.1.1" % "provided",
  "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.7",
  "org.gnieh" % "sohva-entities_2.11" % "1.0.0"
)

// test
libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra-scalatest_2.11" % "2.3.0" % "test",
  "org.eclipse.jetty" % "jetty-runner" % "9.2.5.v20141112" % "test",
  "com.zaxxer" % "HikariCP-java6" % "2.2.5" % "test",
  "ch.qos.logback" % "logback-classic" % "1.1.1" % "test"
)

fork in Test := true


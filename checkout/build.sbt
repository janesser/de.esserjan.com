name := "checkout"

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
  "com.zaxxer" % "HikariCP-java6" % "2.2.5" % "test;provided",
  "com.h2database" % "h2" % "1.4.182" % "test;provided",
  "org.apache.commons" % "commons-dbcp2" % "2.0.1" % "provided",
  "org.apache.derby" % "derby" % "10.11.1.1" % "provided",
  "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
  "com.github.nscala-time" % "nscala-time_2.11" % "1.6.0",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.7"
)

// test
libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra-scalatest_2.11" % "2.3.0" % "test",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.7" % "test"
)

fork in Test := true
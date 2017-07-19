name := "common"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.squeryl" % "squeryl_2.11" % "0.9.5-7",
  "org.scalatra" % "scalatra-swagger_2.11" % "2.3.0",
  "org.scalatra" % "scalatra-json_2.11" % "2.3.0",
  "org.json4s" % "json4s-jackson_2.11" % "3.2.10",
  "org.json4s" % "json4s-native_2.11" % "3.2.10" % "provided",
  "org.scalatra" % "scalatra-scalate_2.11" % "2.3.0",
  "org.scalatra" % "scalatra-scalatest_2.11" % "2.3.0" % "test",
  "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
  "com.typesafe.akka" % "akka-actor_2.11" % "2.3.7"
)

libraryDependencies ++= Seq(
  "org.scalatra" % "scalatra-atmosphere_2.11" % "2.3.0" % "test",
  "org.eclipse.jetty.websocket" % "websocket-server" % "9.2.6.v20141205" % "provided",
  "org.eclipse.jetty.websocket" % "websocket-client" % "9.2.6.v20141205" % "test"
)

libraryDependencies ++= Seq(
  "com.orientechnologies" % "orientdb-object" % "1.7.10" % "test",
  "com.orientechnologies" % "orientdb-client" % "1.7.10" % "test",
  "com.orientechnologies" % "orientdb-jdbc" % "1.7" % "test",
  "com.orientechnologies" % "orientdb-server" % "1.7.10" % "test"
)

libraryDependencies ++= Seq(
  "org.gnieh" % "sohva-entities_2.11" % "1.0.0"
)


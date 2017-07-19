package de.esserjan.com.bootstrap

import java.io.{File, FileInputStream, Reader, InputStream}
import java.nio.file.{Paths, Path}
import java.util.Properties

import scala.io.Source

trait PropertiesSupport {
  def propsPath: String

  /**
   * search in classpath, fall-back to working-directory relative
   * works for src/main/resources, sbt test + container
   */
  def reader: Reader = {
    val classpathRelative =
      getClass.getResourceAsStream(s"/$propsPath")

    val workingDirectoryRelative =
      getClass.getResourceAsStream(propsPath)

    Source.fromInputStream(
      if (classpathRelative != null) classpathRelative
      else workingDirectoryRelative
    ).bufferedReader()
  }

  lazy val props: Properties = {
    val p = new Properties
    p.load(reader)
    p
  }
}
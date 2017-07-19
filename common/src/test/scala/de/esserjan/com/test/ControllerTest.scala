package de.esserjan.com.test

import de.esserjan.com.bootstrap.ModuleDefiner
import org.json4s.DefaultFormats
import org.json4s.jackson.Json
import org.scalatest.FunSuiteLike
import org.scalatest.mock.MockitoSugar
import org.scalatra.test.scalatest.ScalatraSuite

trait ControllerTest extends ScalatraSuite with MockitoSugar with FunSuiteLike {
  implicit val swagger = ModuleDefiner.swagger

  val json = Json(DefaultFormats)
}

package de.esserjan.com.bootstrap

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class ModulesTests extends FlatSpec with Matchers {

  ModulesRegistry.toString should "find at least two modules" in {
    new ModulesRegistry {
      modules.size should be >= 2
    }
  }

}

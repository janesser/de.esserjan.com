package de.esserjan.com.rest

trait IdGenerator {
  def id() = System.currentTimeMillis()
}

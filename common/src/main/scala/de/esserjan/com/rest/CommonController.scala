package de.esserjan.com.rest

import org.scalatra.ScalatraBase
import org.squeryl.{Session, SessionFactory}

trait DatabaseSupport {
  self: ScalatraBase =>

  def bindSession(implicit request: javax.servlet.http.HttpServletRequest): Unit = {
    SessionFactory.concreteFactory match {
      case None => // auto-disable database support
      case Some(_) =>
        val session = SessionFactory.newSession
        request.setAttribute("db-session", session)
        session.bindToCurrentThread
    }
  }

  def unbindSession(implicit request: javax.servlet.http.HttpServletRequest): Unit =
    request.getAttribute("db-session") match {
      case null => // ignore
      case s: Session =>
        s.unbindFromCurrentThread
    }
}
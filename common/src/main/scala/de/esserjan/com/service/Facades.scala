package de.esserjan.com.service

import akka.actor.ActorRef
import akka.util.Timeout
import de.esserjan.com.util.AkkaUtil

trait Facade

trait ActorFacade extends Facade {
  def consumer: ActorRef

  implicit def timeout: Timeout

  def await[RESP_MSG, RESULT](msg: Any)
                             (unwrapFun: RESP_MSG => RESULT): RESULT =
    AkkaUtil.awaitFun(consumer, msg)(unwrapFun)
}

case class FacadeFactory(_create: () => Facade) {
  def create[T <: Facade](): T = _create().asInstanceOf[T]
}

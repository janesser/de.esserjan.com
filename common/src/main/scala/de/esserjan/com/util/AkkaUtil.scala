package de.esserjan.com.util

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.util.Timeout
import org.joda.time.Duration

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

object AkkaUtil {
  val DEFAULT_ASK_TIMEOUT = Duration.standardSeconds(10L)

  val timeoutDuration =
    FiniteDuration(
      DEFAULT_ASK_TIMEOUT.getStandardSeconds,
      TimeUnit.SECONDS)

  def ask[RESULT](actorRef: ActorRef, msg: Any)
                 (implicit timeout: Timeout = Timeout(timeoutDuration)): Future[RESULT] =
    akka.pattern.ask(actorRef, msg).asInstanceOf[Future[RESULT]]


  def await[RESULT](actorRef: ActorRef, msg: Any)
                   (implicit timeout: Timeout = Timeout(timeoutDuration)): RESULT =
    Await.result(
      ask(actorRef, msg),
      timeoutDuration)

  def awaitFun[RESP_MSG, RESULT](actorRef: ActorRef, msg: Any)
                                (unwrapFun: RESP_MSG => RESULT)
                                (implicit timeout: Timeout = Timeout(timeoutDuration)): RESULT = {
    val resp = await[RESP_MSG](actorRef, msg)
    unwrapFun(resp)
  }
}


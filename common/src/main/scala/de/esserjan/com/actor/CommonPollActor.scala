package de.esserjan.com.actor

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef}
import akka.routing.FromConfig
import org.joda.time.{DateTime, Duration}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait BidderMessage[REF, RESULT] {
  def ref: REF

  def result: RESULT

  def validUntil: DateTime
}

trait CommonPollActor extends Actor {
  type BC_MSG
  type TO_MSG
  type REF
  type RESULT
  type BID_MSG <: BidderMessage[REF, RESULT]

  import scala.language.implicitConversions

  implicit def toFiniteDuration(jodaDuration: Duration): FiniteDuration =
    FiniteDuration(jodaDuration.getMillis, TimeUnit.MILLISECONDS)

  def pollDuration: Duration

  def minimumLasting: Duration

  implicit def ec: ExecutionContext

  def scheduler = context.system.scheduler

  def broadcastName: String

  def broadcast: ActorRef = context.actorOf(FromConfig.props(), broadcastName)

  def broadcastClaim(bcMsg: BC_MSG, toMsg: TO_MSG): Unit = {
    broadcast ! bcMsg

    // every sender triggers an individual timeout
    scheduler.scheduleOnce(pollDuration, context.self, toMsg)
  }

  val bidderResponses = mutable.HashMap[REF, mutable.Buffer[BID_MSG]]()

  def bufferBidderResponse(response: BID_MSG): Unit = {
    bidderResponses +=
      response.ref ->
        (bidderResponses.getOrElse(response.ref, mutable.Buffer[BID_MSG]()) += response)
  }

  def respondAndClean(ref: REF)(respond: Seq[RESULT] => Unit): Unit = {
    val now = DateTime.now

    // filter sufficient offer lasting
    import scala.language.postfixOps
    val validResponses =
      bidderResponses.getOrElse(ref, Nil) filter {
        case bm: BID_MSG =>
          bm.validUntil.minus(minimumLasting).compareTo(now) > 0
      } toBuffer

    // cleanUp offerResponses
    if (validResponses.isEmpty)
      bidderResponses.remove(ref)
    else
      bidderResponses.update(ref, validResponses)

    // unwrap
    val results: Seq[RESULT] = validResponses map {
      _.result
    }

    respond(results)
  }
}

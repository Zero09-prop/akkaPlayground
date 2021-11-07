package stream.patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object IntegratingActor extends App {
  implicit val system = ActorSystem("WithActor")
  implicit val materializer = ActorMaterializer

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s + s
      case n: Int =>
        log.info(s"Just received an integer: $n")
        sender ! n * 2
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "ActorSimp")
  val numberSource = Source(1 to 10)

  implicit val timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](4)(simpleActor)

  //numberSource.via(actorBasedFlow).runForeach(println)

  val actorPoweredSource = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
  val matValueSource = actorPoweredSource
    .to(Sink.foreach(num =>
      println(s"Actor get number $num"))).run()
  matValueSource ! 10
  matValueSource ! 56

  matValueSource ! akka.actor.Status.Success("complete")


  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.info(s"Stream failed: $ex")
      case mes =>
        log.info(s"Messgae $mes has come to its final resting point")
        sender() ! StreamAck

    }

  }
  val destinationActor = system.actorOf(Props[DestinationActor])

  val actorSink = Sink.actorRefWithBackpressure[Int](
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwbale => StreamFail(throwbale)
  )

  Source(1 to 10).to(actorSink).run
}

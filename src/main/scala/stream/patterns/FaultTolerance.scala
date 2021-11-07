package stream.patterns

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.{ActorAttributes, ActorMaterializer}
import akka.stream.scaladsl.{RestartSource, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.Random

object FaultTolerance extends App {
  implicit val system = ActorSystem("FaultTolerance")
  implicit val materializer = ActorMaterializer

  val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
  //faultySource.log("trackingElements").to(Sink.ignore).run()

  faultySource
    .recover {
      case e: RuntimeException => Int.MinValue
    }
    .log("gracefulSource")
    .to(Sink.ignore)
  //.run()
  faultySource
    .recoverWithRetries(
      3,
      {
        case _: RuntimeException => Source(1 to 99)
      }
    )
    .log("recoverWithRetries")
    .to(Sink.ignore)
  //.run()

  val restartSource = RestartSource.onFailuresWithBackoff(
    minBackoff = 1 second,
    maxBackoff = 30 seconds,
    randomFactor = 0.2
  )(() => {
    val randomNumber = new Random().nextInt(15)
    println(randomNumber)
    Source(1 to 10).map(elem => if (elem == randomNumber) throw new RuntimeException else elem)
  })
  restartSource
    .log("restartBackoff")
    .to(Sink.ignore)
   // .run()

  val numbers = Source(1 to 20).map(n => if (n == 13) throw new RuntimeException else n).log("numbers")
  val numbersGraph = numbers.withAttributes(ActorAttributes.withSupervisionStrategy{
    case _: RuntimeException => Resume
    case _ => Stop
  }).run()
}

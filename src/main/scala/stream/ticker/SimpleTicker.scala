package stream.ticker

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import java.util.TimerTask
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object SimpleTicker extends App {
  implicit val system: ActorSystem = ActorSystem("TickerSystem")
  implicit val materializer: ActorMaterializer.type = ActorMaterializer
  val t = new TimerTask {
    override def run(): Unit = println(6)
  }
  Source.tick(0 milli, 5 seconds,()).map(_ => t.run()).run()
}

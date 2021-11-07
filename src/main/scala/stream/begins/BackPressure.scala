package stream.begins

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration.DurationInt

object BackPressure extends App{
  implicit val system = ActorSystem("backpressure")
  implicit val materializer = ActorMaterializer

  val fastSource = Source(1 to 100)
  val slowSink = Sink.foreach[Int]{ x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }
  //fastSource.to(slowSink).run()

  val simpleFlow = Flow[Int].map{x =>
    println(s"Incoming: $x")
    x + 1
  }
//  fastSource.async
//    .via(simpleFlow).async
//    .to(slowSink).run()
//
//  fastSource.async
//    .via(simpleFlow).async
//    .to(slowSink)
//   // .run()
  val bufferFlow = simpleFlow.buffer(10,OverflowStrategy.backpressure)
  fastSource.async
    .via(bufferFlow).async
    .to(slowSink).run()
  //fastSource.throttle(10,5.second).runForeach(println)

}

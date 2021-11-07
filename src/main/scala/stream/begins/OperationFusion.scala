package stream.begins

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperationFusion extends App{
  implicit val system = ActorSystem("Fusion")
  implicit val materializer = ActorMaterializer

  val simpleSource = Source(1 to 1000)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach(println)

  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink)



}
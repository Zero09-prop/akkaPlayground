package stream.graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializeValues extends App {
  implicit val system = ActorSystem("GraphMaterializer")
  implicit val materializer = ActorMaterializer

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach(println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  val sink = Sink.fromGraph(
    GraphDSL.create(counter) { implicit builder => counterShape =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[String](2))
      val flowFilter = builder.add(
        Flow[String]
          .filter(word => word == word.toLowerCase)
      )
      val flowCount = builder.add(
        Flow[String]
          .filter(_.length < 5)
      )
      broadcast ~> flowFilter ~> printer
      broadcast ~> flowCount ~> counterShape
      SinkShape(broadcast.in)
    }
  )
  import system.dispatcher
//  val future = wordSource.toMat(sink)(Keep.right).run()
//  future.onComplete {
//    case Success(count) => println(s"The total number of short strings is: $count")
//    case Failure(ex)    => println("Failed")
//  }

  def enchanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val sink = Sink.fold[Int, B](0)((c, _) => c + 1)
    Flow.fromGraph(
      GraphDSL.create(sink) { implicit builder => sinkShape =>
        import GraphDSL.Implicits._
        val flowShape = builder.add(flow)
        val broadcast = builder.add(Broadcast[B](2))
        flowShape ~> broadcast ~> sinkShape
        FlowShape(flowShape.in,broadcast.out(1))
      }
    )
  }
  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(identity)
  val simpleSink = Sink.ignore

  val futureFlow = simpleSource.viaMat(enchanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()
  futureFlow.onComplete{
    case Success(c) => println(c)
    case Failure(ex) => println(ex)
  }
}

package stream.graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}

object OpenGraphs extends App {
  implicit val system = ActorSystem("OpenGraphsSys")
  implicit val materializer = ActorMaterializer

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val graph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )
  //graph.runForeach(println)

  val sink1 = Sink.foreach[Int](x => println(s"Sink1: ${x}"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink2: ${x}"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )
  //firstSource.runWith(sinkGraph)

  val firstFlow = Flow[Int].map(_ + 1)
  val secondFlow = Flow[Int].map(_ * 10)
  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
     // val broadcast = builder.add(Broadcast[Int](2))
      val incShape = builder.add(firstFlow)
      val mulShape = builder.add(secondFlow)
//      val zip = builder.add(Zip[Int, Int])
      incShape ~> mulShape
//      broadcast ~> firstFlow ~> zip.in0
//      broadcast ~> secondFlow ~> zip.in1
//      FlowShape(broadcast.in,zip.out)
      FlowShape(incShape.in, mulShape.out)
    }
  )
  //flowGraph.runWith(firstSource, Sink.foreach(println))

  val flowSinkSource = Flow.fromGraph(
    GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._
      val source = builder.add(firstSource)
      val sink = builder.add(sink1)
      FlowShape(sink.in,source.out)
    }
  )

}

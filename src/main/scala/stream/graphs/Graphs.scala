package stream.graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object Graphs extends App {
  implicit val system = ActorSystem("GraphSys")
  implicit val materializer = ActorMaterializer

  val input = Source(1 to 1000)
  val increment = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)
  val output = Sink.foreach[(Int, Int)](println)
  val output1 = Sink.foreach[Int](x => println(s"Sink1: ${x}"))
  val output2 = Sink.foreach[Int](x => println(s"Sink2: ${x}"))

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int])
      input ~> broadcast
      broadcast.out(0) ~> increment ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> output
      ClosedShape
    }
  )
  //graph.run()

  val graphSinks = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val broadcast = builder.add(Broadcast[Int](2))
      input ~> broadcast
      broadcast ~> output1
      broadcast ~> output2
      ClosedShape
    }
  )
  //graphSinks.run()

  val graph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val merge = builder.add(Merge[Int](2))
      val source2 = Source(1000 to 2000)
      val balance = builder.add(Balance[Int](2))
      input ~> merge
      source2 ~> merge
      merge ~> balance ~> output1
      balance ~> output2
      ClosedShape
    }
  )
  graph2.run()
}

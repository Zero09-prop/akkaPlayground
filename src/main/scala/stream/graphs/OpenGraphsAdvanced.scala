package stream.graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.stream.{ActorMaterializer, AmorphousShape, ClosedShape, FanInShape, FlowShape, UniformFanInShape}

object OpenGraphsAdvanced extends App {
  implicit val system = ActorSystem("AdvancedGraph")
  implicit val materializer = ActorMaterializer

  val max3Comp = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((z, b) => math.max(z, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((z, b) => math.max(z, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }
  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is: ${x}"))

  case class Transaction(id: Int, amount: Int)

  val max3Graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3 = builder.add(max3Comp)
      source1 ~> max3
      source2 ~> max3
      source3 ~> max3
      max3 ~> maxSink
      ClosedShape
    }
  )
  //max3Graph.run()

  val sourceTransaction = Source(List(Transaction(1, 100), Transaction(2, 1500), Transaction(3, 19000)))
  val transact = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._
    val broadcast = builder.add(Broadcast[Transaction](2))
    val transaction = builder.add(Flow[Transaction].map(identity))
    val validation = builder.add(Flow[Transaction].map(t => if (t.amount > 10000) t.id else 0))
    val zip = builder.add(Zip[Transaction, Int])
    broadcast ~> transaction ~> zip.in0
    broadcast ~> validation ~> zip.in1
    FlowShape(broadcast.in, zip.out)
  }
  val sink = Sink.foreach[(Transaction, Int)](println)
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val mechanism = builder.add(transact)
      sourceTransaction ~> mechanism ~> sink
      ClosedShape
    }
  )
  graph.run()
}

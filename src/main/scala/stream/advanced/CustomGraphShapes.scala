package stream.advanced

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ClosedShape, Graph, Inlet, Outlet, Shape}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object CustomGraphShapes extends App {
  implicit val system = ActorSystem("CustomGraphShapes")

  case class BalanceMxN[T](
      override val inlets: List[Inlet[T]],
      override val outlets: List[Outlet[T]]
  ) extends Shape {
    override def deepCopy(): Shape =
      BalanceMxN(
        inlets.map(_.carbonCopy()),
        outlets.map(_.carbonCopy())
      )
  }
  object BalanceMxN {
    def apply[T](inputCount: Int, outputCount: Int): Graph[BalanceMxN[T], NotUsed] =
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val merge = builder.add(Merge[T](inputCount))
        val balance = builder.add(Balance[T](outputCount))

        merge.out ~> balance.in

        BalanceMxN(merge.inlets.toList, balance.outlets.toList)
      }
  }

//  val balance2x3Impl = GraphDSL.create() { implicit builder =>
//    import GraphDSL.Implicits._
//
//    val merge = builder.add(Merge[Int](2))
//    val balance = builder.add(Balance[Int](3))
//
//    merge.out ~> balance.in
//
//    BalanceMxN(List(merge.in(0), merge.in(1)), List(balance.out(0), balance.out(1), balance.out(2)))
//  }

//  val balance2x3Graph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//
//      val slowSource = Source(LazyList.from(1)).throttle(1, 1 second)
//      val fastSource = Source(LazyList.from(1)).throttle(2, 1 second)
//
//      def createSink(index: Int) =
//        Sink.fold[Int, Int](0)((count, el) => {
//          println(s"[sink $index] Received ${el}, current count is $count")
//          count + 1
//        })
//      val sink1 = builder.add(createSink(1))
//      val sink2 = builder.add(createSink(2))
//      val sink3 = builder.add(createSink(3))
//
//      val balance2x3 = builder.add(balance2x3Impl)
//
//      slowSource ~> balance2x3.myInlets.head
//      fastSource ~> balance2x3.myInlets(1)
//      balance2x3.myOutlets.head ~> sink1
//      balance2x3.myOutlets(1) ~> sink2
//      balance2x3.myOutlets(2) ~> sink3
//
//      ClosedShape
//    }
//  )

  //balance2x3Graph.run()

  val balanceMxNGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val slowSource = Source(LazyList.from(1)).throttle(1, 1 second)
      val fastSource = Source(LazyList.from(1)).throttle(2, 1 second)

      def createSink(index: Int) =
        Sink.fold[Int, Int](0)((count, el) => {
          println(s"[sink $index] Received $el, current count is $count")
          count + 1
        })
      val sink1 = builder.add(createSink(1))
      val sink2 = builder.add(createSink(2))
      val sink3 = builder.add(createSink(3))

      val balance2x3 = builder.add(BalanceMxN[Int](2,3))
      slowSource ~> balance2x3.inlets.head
      fastSource ~> balance2x3.inlets(1)
      balance2x3.outlets.head ~> sink1
      balance2x3.outlets(1) ~> sink2
      balance2x3.outlets(2) ~> sink3

      ClosedShape
    }
  )
  balanceMxNGraph.run()
}

package stream.graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}
import akka.stream.{ActorMaterializer, ClosedShape, FanInShape2, OverflowStrategy, UniformFanInShape}

object GraphCycles extends App {
  implicit val system = ActorSystem("GraphCycle")
  implicit val materializer = ActorMaterializer
  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating ${x}")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape
    ClosedShape
  }
  //RunnableGraph.fromGraph(accelerator).run()

  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating ${x}")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape.preferred <~ incrementerShape
    ClosedShape
  }

  //RunnableGraph.fromGraph(actualAccelerator).run()

  val bufferedAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].buffer(10,OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating ${x}")
      Thread.sleep(100)
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
    mergeShape <~ incrementerShape
    ClosedShape
  }

  val FibShape = GraphDSL.create(){implicit builder =>
    import GraphDSL.Implicits._
    val zip = builder.add(Zip[BigInt,BigInt])
    val mergePred = builder.add(MergePreferred[(BigInt,BigInt)](1))
    val fibLogic = builder.add(Flow[(BigInt,BigInt)].map{pair =>
      val last = pair._1
      val previous = pair._2
      Thread.sleep(100)
      (last + previous,last)
    })
    val broadcast = builder.add(Broadcast[(BigInt,BigInt)](2))
    val extractLast = builder.add(Flow[(BigInt,BigInt)].map(_._1))
    zip.out ~> mergePred ~> fibLogic ~> broadcast ~> extractLast
                mergePred <~ broadcast
    UniformFanInShape(extractLast.out,zip.in0,zip.in1)
  }

  val fibGraph = RunnableGraph.fromGraph(
    GraphDSL.create(){implicit builder =>
      import GraphDSL.Implicits._
      val source1 = builder.add(Source.single[BigInt](1))
      val source2 = builder.add(Source.single[BigInt](1))
      val sink = builder.add(Sink.foreach[BigInt](println))
      val fibo = builder.add(FibShape)

      source1 ~> fibo
      source2 ~> fibo ~> sink
      ClosedShape
    }
  )
  fibGraph.run()

}

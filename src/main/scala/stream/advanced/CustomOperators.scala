package stream.advanced

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Attributes, FlowShape, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success, Try}

object CustomOperators extends App {
  implicit val system = ActorSystem("CustomOperators")
  implicit val materializer = ActorMaterializer
  import system.dispatcher
  class RandomNumberGenerator(max: Int) extends GraphStage[SourceShape[Int]] {
    val outPort = Outlet[Int]("randomGenerator")
    val random = new Random()

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(
          outPort,
          new OutHandler {
            override def onPull(): Unit = {
              val nextNumber = Random.nextInt(max)
              push(outPort, nextNumber)
            }
          }
        )
      }
    override def shape: SourceShape[Int] = SourceShape(outPort)

  }

  val randomGeneratorSource = Source.fromGraph(new RandomNumberGenerator(100))
  //randomGeneratorSource.runForeach(println)

  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]] {
    val inPort = Inlet[Int]("batcher")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        val batch = new mutable.Queue[Int]

        override def preStart(): Unit = {
          pull(inPort)
        }
        setHandler(
          inPort,
          new InHandler {
            override def onPush(): Unit = {
              val nextElement = grab(inPort)
              batch.enqueue(nextElement)
              if (batch.size >= batchSize)
                println("New batch: " + batch.dequeueAll(_ => true).mkString("[", ",", "]"))

              pull(inPort)
            }

            override def onUpstreamFinish(): Unit = {
              if (batch.nonEmpty) {
                println("New batch: " + batch.dequeueAll(_ => true).mkString("[", ",", "]"))
                println("Stream finished")
              }
            }
          }
        )
      }
    override def shape: SinkShape[Int] = SinkShape(inPort)

  }

  val batcherSink = Sink.fromGraph(new Batcher(10))
  //randomGeneratorSource.runWith(batcherSink)

  class Filter[T](pred: T => Boolean) extends GraphStage[FlowShape[T, T]] {

    val inPort = Inlet[T]("flowInput")
    val outPort = Outlet[T]("flowOutput")

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandlers(
          inPort,
          outPort,
          new InHandler with OutHandler {

            override def onPush(): Unit = {
              Try {
                val element = grab(inPort)
                if (pred(element)) push[T](outPort, element)
                else {
                  pull(inPort)
                }
              } match {
                case Success(_)         =>
                case Failure(exception) => failStage(exception)
              }
            }

            override def onPull(): Unit = {
              pull[T](inPort)
            }
          }
        )
      }

    override def shape: FlowShape[T, T] = FlowShape[T, T](inPort, outPort)
  }

  val flowShape = Flow.fromGraph(new Filter[Int](_ % 2 == 0))

  //randomGeneratorSource.via(flowShape).runWith(batcherSink)

  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T, T], Future[Int]] {

    val inPort = Inlet[T]("counterInput")
    val outPort = Outlet[T]("counterOutput")

    override def shape = FlowShape(inPort, outPort)

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]
      val logic = new GraphStageLogic(shape) {
        var counter = 0

        setHandlers(
          inPort,
          outPort,
          new InHandler with OutHandler {
            override def onPush(): Unit = {
              val element = grab(inPort)
              counter += 1
              push(outPort, element)
            }

            override def onDownstreamFinish(): Unit = {
              promise.success(counter)
              super.onDownstreamFinish()
            }

            override def onUpstreamFinish(): Unit = {
              promise.success(counter)
              super.onUpstreamFinish()
            }

            override def onUpstreamFailure(ex: Throwable): Unit = {
              promise.failure(ex)
              super.onUpstreamFailure(ex)
            }

            override def onDownstreamFinish(cause: Throwable): Unit = {
              promise.failure(cause)
              super.onDownstreamFinish(cause)
            }
            override def onPull(): Unit = pull(inPort)
          }
        )
      }
      (logic, promise.future)
    }
  }

  val counterFlow = Flow.fromGraph(new CounterFlow[Int])
  val countFuture = Source(1 to 10)
    //.map(x => if (x == 7) throw new RuntimeException else x)
    .viaMat(counterFlow)(Keep.right)
    //.to(Sink.foreach(println))
    .to(Sink.foreach[Int](x => if(x == 7) throw new RuntimeException else println(x)))
    .run()

  countFuture.onComplete {
    case Success(count) => println(s"The number of elements: $count")
    case Failure(ex)    => println(s"Counting elements failed: $ex")
  }
}

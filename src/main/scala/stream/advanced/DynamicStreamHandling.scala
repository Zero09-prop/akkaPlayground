package stream.advanced

import akka.actor.ActorSystem
import akka.stream.KillSwitches
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object DynamicStreamHandling extends App {
  implicit val system = ActorSystem("DynamicStreamHandling")
  import system.dispatcher
  val killSwitchFlow = KillSwitches.single[Int]
  val counter = Source(LazyList.from(1)).throttle(1, 1 second).log("Counter")
  val sink = Sink.ignore

//  val killSwitch = counter
//    .viaMat(killSwitchFlow)(Keep.right)
//    .to(sink)
//    .run()
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    killSwitch.shutdown()
//  }

//  val anotherCounter = Source(LazyList.from(1)).throttle(2, 1 second).log("AnotherCounter")
//  val sharedKillSwitch = KillSwitches.shared("OneButtonToRuleThemAll")
//
//  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    sharedKillSwitch.shutdown()
//  }

  val dynamicMerge = MergeHub.source[Int]
  val dynamicBroad = BroadcastHub.sink[Int]
//  val materializedSink = dynamicMerge.to(Sink.foreach(println)).run()
//  Source(1 to 10).runWith(materializedSink)
  val som = dynamicMerge.toMat(dynamicBroad)(Keep.both).run()

  val a = som._2.runFold(0)(_ + _)

  Source(1 to 10).to(som._1).run()
  Await.result(a,3 seconds)
  a.onComplete {
    case Success(value)     => println(s"My value $value")
    case Failure(exception) => throw new RuntimeException
  }

}

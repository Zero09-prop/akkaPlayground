package stream.advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object SubStreams extends App {

  implicit val system = ActorSystem("SubStream")
  import system.dispatcher
  val wordSource = Source(List("Akka", "is", "amazing", "leaning", "substreams"))
  val groups = wordSource.groupBy(30, word => if (word.isEmpty) '\u0000' else word.toLowerCase.charAt(0))

  groups
    .to(Sink.fold(0)((count, word) => {
      val newCount = count + 1
      println(s"I just received $word, count is $newCount")
      newCount
    }))
  //.run()

  val textSource = Source(
    List(
      "I love Akka Streams",
      "this is amazing",
      "learning from Rock the JVM"
    )
  )

  val totalCharCounterFuture = textSource
    .groupBy(2, string => string.length % 2)
    .map(_.length)
    .mergeSubstreamsWithParallelism(2)
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  totalCharCounterFuture.onComplete {
    case Success(value)     => println(s"Total char count: $value")
    case Failure(exception) => println(s"Char computation failed: $exception")
  }

  val text = "I love Akka Streams\n" +
  "this is amazing\n" +
  "learning from Rock the JVM\n"

  val anotherCharCounFuture = Source(text.toList)
    .splitWhen(c => c == '\n')
    .filter(_ != '\n')
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCharCounFuture.onComplete{
    case Success(value)     => println(s"Total char count: $value")
    case Failure(exception) => println(s"Char computation failed: $exception")
  }

  val simpleSource = Source(1 to 5)
  //simpleSource.flatMapConcat(x => Source(x to 3 * x)).runForeach(println)
  simpleSource.flatMapMerge(2,x => Source(x to 3 * x)).runForeach(println)


}

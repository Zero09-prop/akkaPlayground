package stream.begins

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher
  val sentences = List("Pop", "Rom", "Mir", "dom", "home", "from", "gnome", "tom", "some")
//  val result1 = Source(sentences).runWith(Sink.last).onComplete{
//    case Success(word) => println(s"Your word is '$word'")
//    case Failure(exception) => println(s"You have exception: $exception")
//  }
  val result2 = Source(sentences).runFold(0)((word,sentence) => word + sentence.split(" ").length).onComplete{
  case Success(a) => println(a)
  case Failure(e) => println(e)
}


}

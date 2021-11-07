package stream.begins

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime

import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object FirstPrinciple extends App{
  implicit val system = ActorSystem("FirstPrinciple")
  implicit val materializer = ActorMaterializer()
  val names = List("misha","bob","jim","sdsdsdsdsd","qwertyuu","1233","0090090909090")
  val source = Source(names).filter(_.length >= 5).take(2)
  val sourceNumbers = Source(1 to 100)
  val result: Future[Int] = sourceNumbers.runFold(0)((acc, el) => acc + el)
  Await.result(result,5 seconds)
  println(result)

}

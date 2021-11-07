package stream.patterns

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import java.util.Date
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object IntegratingExternal extends App {
  implicit val system = ActorSystem("IntegrateSys")
  implicit val materializer = ActorMaterializer
  import system.dispatcher
//  def genericExtService[A, B](element: A): Future[B] = ???

  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(
    List(
      PagerEvent("AkkaInfra", "Infrastructure broke", new Date),
      PagerEvent("FastDataPipeline", "Illegal elements in the pipeline", new Date),
      PagerEvent("AkkaInfra", "A service stopped responding", new Date),
      PagerEvent("SuperFrontend", "A button doesn't work", new Date)
    )
  )

  object PagerService {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rtjvm.com",
      "John" -> "john@rtjvm.com",
      "Lady Gaga" -> "ladygag@rtjvm.com"
    )
    def processEvent(pagerEvent: PagerEvent) =
      Future {
        val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
        val engineer = engineers(engineerIndex.toInt)
        val engineerEmail = emails(engineer)
        println(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
        Thread.sleep(1000)
        engineerEmail
      }
  }
  val infraEvents = eventSource.filter(_.application == "AkkaInfra")
  val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(PagerService.processEvent)
  val pagedEmailSink = Sink.foreach[String](email => println(s"Successfully sent notification to $email"))

  //pagedEngineerEmails.to(pagedEmailSink).run()

  class PagerActor extends Actor {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@rtjvm.com",
      "John" -> "john@rtjvm.com",
      "Lady Gaga" -> "ladygag@rtjvm.com"
    )
    private def processEvent(pagerEvent: PagerEvent) = {
      val engineerIndex = (pagerEvent.date.toInstant.getEpochSecond / (24 * 3600)) % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)
      system.log.info(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")
      Thread.sleep(1000)
      engineerEmail

    }
    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }
  import akka.pattern.ask
  implicit val timeout = Timeout(3 second)
  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
  val alternativePagedEngineerEmails = infraEvents.mapAsync(4)(event => (pagerActor ? event).mapTo[String])
  alternativePagedEngineerEmails.to(pagedEmailSink).run()
}

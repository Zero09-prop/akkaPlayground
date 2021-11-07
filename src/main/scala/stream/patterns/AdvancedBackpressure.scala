package stream.patterns

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

import java.util.Date
import scala.language.postfixOps

object AdvancedBackpressure extends App {
  implicit val system = ActorSystem("AdvancedBackpressure")
  implicit val materializer = ActorMaterializer

  val controlledFlow = Flow[Int].map(_ * 2).buffer(10,OverflowStrategy.dropHead)

  case class PagerEvent(description: String,date: Date, nInstances: Int = 1)
  case class Notification(email: String,event: PagerEvent)

  val events = List(
    PagerEvent("Service discovery failed",new Date),
    PagerEvent("Illegal elements in the data pipeline", new Date),
    PagerEvent("Number of HTTP 500 spiked", new Date),
    PagerEvent("A service stopped responding",new Date)
  )
  val eventSource = Source(events)

  val oncallEngineer = "misha@mail.com"

  def sendEmail(notification: Notification) =
    println(s"Dear ${notification.email}, you have an event ${notification.event}")

  val notificationSink = Flow[PagerEvent].map(event => Notification(oncallEngineer,event))
    .to(Sink.foreach[Notification](sendEmail))

  //eventSource.to(notificationSink).run()

  def sendEmailSlow(notification: Notification) = {
    Thread.sleep(1000)
    println(s"Dear ${notification.email}, you have an event ${notification.event}")
  }
  val aggreagateNotificationFlow = Flow[PagerEvent]
    .conflate((event1,event2) => {
      val nInstances = event1.nInstances + event2.nInstances
        PagerEvent(s"You have $nInstances events that requei",new Date,nInstances)
    })
    .map(event => Notification(oncallEngineer,event))
  //eventSource.via(aggreagateNotificationFlow).async.to(Sink.foreach(sendEmailSlow)).run()

  import scala.concurrent.duration._
  val slowCounter = Source(LazyList.from(1)).throttle(1,1 second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(element => Iterator.from(element))
  val repeater = Flow[Int].extrapolate(element => Iterator.continually(element))
  val expander = Flow[Int].expand(element => Iterator.from(element))
  slowCounter.via(expander).to(hungrySink).run()
}

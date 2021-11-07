package stream.graphs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object BiDirect extends App {
  implicit val sys = ActorSystem("Bi")
  implicit val mater = ActorMaterializer

}

package stream

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, Framing, GraphDSL, RunnableGraph, Sink}
import akka.util.ByteString
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import java.io.{File, FileWriter}
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success}

object PipelineDB extends App {
  implicit val system: ActorSystem = ActorSystem("PipeLineDB")

  val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("slick-postgres")
  implicit val session: SlickSession = SlickSession.forConfig(databaseConfig)
  import system.dispatcher
  system.registerOnTermination(() => session.close())

  case class Student(id: Int, name: String, age: Int)
  def createStudent(string: String): Student = {
    val data = string.split(";")
    val dateBirth = LocalDate.parse(data(2), DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    Student(data.head.toInt, data(1), LocalDate.now().getYear - dateBirth.getYear)
  }
  val file = Paths.get("./example.csv")
  val fileWriter = new FileWriter(new File("./students.csv"))
  import session.profile.api._
  val source = FileIO.fromPath(file)
  val tableName = "Students"
  val sinkDB =
    Slick.sink((student: Student) => sqlu"INSERT INTO Students VALUES(${student.id},${student.name}, ${student.age})")

  val sinkFile = Sink.foreach[String](st => fileWriter.write(st + "\n"))
  val graph = RunnableGraph.fromGraph(
    GraphDSL.createGraph(source, sinkDB, sinkFile)((_, _, M3) => M3) {
      implicit builder => (sourceShape, sink1Shape, sink2Shape) =>
        import GraphDSL.Implicits._
        val batch =
          Framing.delimiter(ByteString(System.lineSeparator), maximumFrameLength = 8192, allowTruncation = true)
        val decoder = Flow[ByteString].map(_.utf8String)
        val creatorStudent = Flow[String].map(createStudent)
        val broadcast = builder.add(Broadcast[String](2))
        sourceShape ~> batch ~> decoder ~> broadcast ~> creatorStudent ~> sink1Shape
        broadcast ~> sink2Shape
        ClosedShape
    }
  )
  val result = graph.run()
  result.onComplete {
    case Success(_) =>
      println(s"Program end successfully")
      fileWriter.close()
    case Failure(ex) =>
      println(s"Error is program crashed, because $ex")
      fileWriter.close()
  }
}

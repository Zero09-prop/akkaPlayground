name := "akkaPlayground"

version := "0.1"

scalaVersion := "2.13.6"
val akkaVersion = "2.6.16"
val scalaTestVersion = "3.3.0-SNAP3"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "3.0.3",
  "org.postgresql" % "postgresql" % "42.2.24"
)

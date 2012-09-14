import sbt._
import sbt.Keys._

object WorkflowBuild extends Build {

  lazy val workflow = Project(
    id = "workflow",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "Workflow",
      organization := "com.zuehlke.functional.example",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq(
          "com.typesafe.akka" % "akka-actor" % "2.0.1"
          
      )
      
    )
  )
}

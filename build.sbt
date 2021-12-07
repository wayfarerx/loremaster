import Dependencies._

lazy val root = project.in(file(".")).settings(
  organization := "net.wayfarerx",
  name := "loremaster",
  version := "0.1.0",

  scalaVersion := Scala3Version,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores"),

  libraryDependencies ++= Seq(
    // API
    CatsCore,
    CirceCore,
    CirceGeneric,
    CirceParser,
    Scalactic,
    Zio,
    // AWS
    AwsLambdaCore,
    AwsLambdaEvents,
    // Tweeting
    Twitter4j,
    // Testing
    ScalaTest,
    Mockito
  )
)
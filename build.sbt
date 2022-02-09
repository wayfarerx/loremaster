import Dependencies._

enablePlugins(PublishToS3)

lazy val root = project.in(file(".")).settings(
  organization := "net.wayfarerx",
  name := "loremaster",
  version := "0.1.0",

  scalaVersion := Scala3Version,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores"),

  libraryDependencies ++= Seq(
    // Core
    CatsCore,
    CirceCore,
    CirceGeneric,
    CirceParser,
    Zio,
    // AWS
    AwsLambdaCore,
    AwsLambdaEvents,
    AwsSqs,
    // Testing
    ScalaTest,
    Mockito
  ),

  s3Bucket := "loremaster-code"

)
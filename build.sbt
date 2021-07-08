import Dependencies._

lazy val root = project.in(file(".")).settings(
  organization := "net.wayfarerx",
  name := "loremaster",
  version := "0.1.0",

  scalaVersion := Scala3Version,
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores"),

  libraryDependencies ++= Seq(CirceCore, CirceGeneric, CirceParser, Scopt, Zio, ZioTest, ZioTestSbt),
  testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)

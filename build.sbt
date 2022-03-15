import Dependencies._

enablePlugins(PublishToS3)

ThisBuild / organization := "net.wayfarerx"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := Scala3Version
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores")

/** The Loremaster project. */
lazy val loremaster = (project in file(".")).aggregate(
  core,
  deploy,
  deployTwitter,
  main
)

/** The Loremaster core project. */
lazy val core = project.settings(
  name := "loremaster-core",

  libraryDependencies ++= Seq(
    CatsCore,
    CirceCore,
    CirceGeneric,
    CirceParser,
    Zio,
    ScalaTest,
    Mockito
  )

)

/** The Loremaster deploy project. */
lazy val deploy = project.settings(
  name := "loremaster-deploy",

  libraryDependencies ++= Seq(
    AwsLambdaCore,
    AwsLambdaEvents,
    AwsSqs,
    ScalaTest,
    Mockito
  )

).dependsOn(core)

/** The Loremaster deploy Twitter project. */
lazy val deployTwitter = project.in(file("deploy-twitter")).settings(
  name := "loremaster-deploy-twitter",

  libraryDependencies ++= Seq(
    ScalaTest,
    Mockito
  )
).dependsOn(deploy)

/** The main project. */
lazy val main = project.settings(
  name := "loremaster-main",
).dependsOn(
  deployTwitter
)
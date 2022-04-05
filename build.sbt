import Dependencies._

enablePlugins(SbtProguard)
enablePlugins(PublishToS3)

ThisBuild / organization := "net.wayfarerx"
ThisBuild / version := "0.1.0"
ThisBuild / scalaVersion := Scala3Version
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores")

/** The name of the application. */
lazy val Application = "loremaster"

/** The name of the package. */
lazy val Package = s"net.wayfarerx.$Application"

/** The Loremaster project. */
lazy val loremaster = (project in file(".")).aggregate(
  core,
  deploy,
  deployTwitter,
  main
)

/** The Loremaster Core project. */
lazy val core = project.in(file("core")).settings(
  Seq(
    name := s"$Application-core",
    libraryDependencies ++= Seq(
      CatsCore,
      CirceCore,
      CirceGeneric,
      CirceParser,
      Zio
    )
  ),
  withTesting
)

/** The Loremaster Deploy project. */
lazy val deploy = project.in(file("deploy")).settings(
  Seq(
    name := s"$Application-deploy",
    libraryDependencies ++= Seq(
      AwsLambdaCore,
      AwsLambdaEvents,
      AwsSqs
    )
  ),
  withTesting
).dependsOn(core)

/** The Loremaster Deploy Twitter project. */
lazy val deployTwitter = project.in(file("deploy-twitter")).settings(
  Seq(
    name := s"$Application-deploy-twitter"
  ),
  withProguard(s"$Package.twitter.deployment.TwitterFunction")
).dependsOn(core, deploy)

/** The Loremaster Main project. */
lazy val main = project.in(file("main")).settings(
  name := s"$Application-main"
).dependsOn(
  deployTwitter
)

/**
 * Definition of the testing settings.
 *
 * @return The testing settings.
 */
def withTesting: Seq[sbt.Def.Setting[_]] =
  Seq(libraryDependencies ++= Seq(ScalaTest, Mockito))

/**
 * Definition of the Proguard settings.
 *
 * @param mainClass      The main class to preserve.
 * @param proguardMemory The amount of memory that Proguard gets to work with.
 * @return The Proguard settings.
 */
def withProguard(mainClass: String, proguardMemory: String = "2G"): Seq[sbt.Def.Setting[_]] = {
  val xmx = s"-Xmx$proguardMemory"
  Seq(
    proguard / javaOptions := Seq(xmx),
    Proguard / proguard / javaOptions := Seq(xmx),
    Proguard / proguardMerge := true,
    Proguard / proguardOptions ++= Seq("-dontobfuscate", "-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings"),
    Proguard / proguardOptions += ProguardOptions.keepMain(mainClass),
    Proguard / proguardInputs := (Compile / dependencyClasspath).value.files,
    Proguard / proguardFilteredInputs ++= ProguardOptions.noFilter((Compile / packageBin).value),
    Proguard / proguardMergeStrategies += ProguardMerge.discard("META-INF/.*".r)
  )
}

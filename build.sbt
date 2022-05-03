import Dependencies._

ThisBuild / organization := "net.wayfarerx"
ThisBuild / version := "0.1.3"
ThisBuild / scalaVersion := Scala3Version
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Ykind-projector:underscores")

/** The name of the application. */
lazy val Application = "loremaster"

/** The name of the package. */
lazy val Package = s"net.wayfarerx.$Application"

/** The "core" string". */
lazy val Core = "core"

/** The "deployments" string". */
lazy val Deployments = "deployments"

/** The "twitter" string". */
lazy val Twitter = "twitter"

/** The "main" string". */
lazy val Main = "main"

/** The key for shipping the current build. */
lazy val ship = taskKey[Unit]("Ships the current build")

/**
 * The settings used for all projects.
 *
 * @param domain The domain that the project implements.
 * @return The settings used for all projects.
 */
def commonSettings(domain: String): Seq[Def.Setting[_]] = Seq(
  name := s"$Application-$domain"
)

/**
 * The settings used for library projects.
 *
 * @param domain The domain that the library implements.
 * @return The settings used for library projects.
 */
def librarySettings(domain: String): Seq[Def.Setting[_]] =
  commonSettings(domain) :+ (ship := Def.unit(None))

/**
 * The settings used for Lambda function projects.
 *
 * @param domain           The domain that the function implements.
 * @param proguardHeapSize The maximum heap size to allow for Proguard, defaults to "2G".
 * @return The settings used for Lambda function projects.
 */
def functionSettings(domain: String, proguardHeapSize: String = "2G"): Seq[Def.Setting[_]] = {
  val proguardJavaOptions = Seq(s"-Xmx$proguardHeapSize")
  val mainFunction = s"$Package.$domain.deployment.${domain.capitalize}Function"
  commonSettings(domain) ++ Seq(
    proguard / javaOptions := proguardJavaOptions,
    Proguard / proguard / javaOptions := proguardJavaOptions,
    Proguard / proguardMerge := true,
    Proguard / proguardOptions ++= Seq("-dontobfuscate", "-dontoptimize", "-dontnote", "-dontwarn", "-ignorewarnings"),
    Proguard / proguardOptions += ProguardOptions.keepMain(mainFunction),
    Proguard / proguardInputs := (Compile / dependencyClasspath).value.files,
    Proguard / proguardFilteredInputs ++= ProguardOptions.noFilter((Compile / packageBin).value),
    Proguard / proguardMergeStrategies += ProguardMerge.discard("META-INF/.*".r),
    s3Bucket := s"$Application-lambda-functions",
    s3Key := s"$domain/$Application-$domain-${version.value}.jar",
    uploadedArtifact := (Proguard / proguardOutputs).map(_.head).value,
    ship := Def.sequential(Proguard / proguard, publish).value
  )
}

/** The settings used for testing. */
lazy val testSettings: Seq[Def.Setting[_]] = Seq(libraryDependencies ++= Seq(ScalaTest, Mockito))

/** The Loremaster project. */
lazy val loremaster = project.in(file(".")).aggregate(
  core,
  deployments,
  twitter,
  main
)

/** The Loremaster core project. */
lazy val core = project.in(file(Core))
  .settings(
    librarySettings(Core),
    libraryDependencies ++= Seq(
      CatsCore,
      CirceCore,
      CirceGeneric,
      CirceParser,
      Twitter4JCore,
      Zio
    ),
    testSettings
  )

/** The Loremaster deployments project. */
lazy val deployments = project.in(file(Deployments))
  .settings(
    librarySettings(Deployments),
    libraryDependencies ++= Seq(
      AwsLambdaCore,
      AwsLambdaEvents,
      AwsSqs
    ),
    testSettings
  ).dependsOn(core)

/** The Loremaster Twitter project. */
lazy val twitter = project.in(file(Twitter))
  .enablePlugins(SbtProguard, PublishToS3)
  .settings(functionSettings(Twitter))
  .dependsOn(core, deployments)

/** The Loremaster main project. */
lazy val main = project.in(file(Main))
  .enablePlugins(CloudFormationStack)
  .settings(
    commonSettings(Main),
    Compile / run / mainClass := Some(s"$Package.$Main.${Main.capitalize}"),
    stackName := Application.capitalize,
    stackParameters := List("Version" -> version.value),
    ship := deployStack.value
  ).dependsOn(
  twitter
)
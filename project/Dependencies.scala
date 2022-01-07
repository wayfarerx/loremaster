import sbt._

object Dependencies {

  def Scala3Version = "3.0.0"

  // API

  def CatsGroup = "org.typelevel"
  def CatsVersion = "2.6.1"
  lazy val CatsCore = CatsGroup %% "cats-core" % CatsVersion

  def CirceGroup = "io.circe"
  def CirceVersion = "0.14.1"
  lazy val CirceCore = CirceGroup %% "circe-core" % CirceVersion
  lazy val CirceGeneric = CirceGroup %% "circe-generic" % CirceVersion
  lazy val CirceParser = CirceGroup %% "circe-parser" % CirceVersion

  def ScalacticGroup = "org.scalactic"
  def ScalacticVersion = "3.2.10"
  lazy val Scalactic = ScalacticGroup %% "scalactic" % ScalacticVersion

  def ZioGroup = "dev.zio"
  def ZioVersion = "1.0.12"
  lazy val Zio = ZioGroup %% "zio" % ZioVersion

  // AWS

  def AwsGroup = "com.amazonaws"
  def AwsVersion = "1.2.1"
  lazy val AwsLambdaCore = AwsGroup % "aws-lambda-java-core" % AwsVersion
  lazy val AwsLambdaEvents = AwsGroup % "aws-lambda-java-events" % "3.10.0"
  lazy val AwsSqs = AwsGroup % "amazon-sqs-java-messaging-lib" % "1.0.4"

  // Tweeting

  def Twitter4jGroup = "org.twitter4j"
  def Twitter4jVersion = "4.0.7"
  lazy val Twitter4j = Twitter4jGroup % "twitter4j-core" % Twitter4jVersion

  // Testing

  def ScalaTestGroup = "org.scalatest"
  def ScalaTestVersion = "3.2.10"
  lazy val ScalaTest = ScalaTestGroup %% "scalatest" % ScalaTestVersion % Test

  def MockitoGroup = "org.scalatestplus"
  def MockitoVersion = "3.2.10.0"
  lazy val Mockito = MockitoGroup %% "mockito-3-4" % MockitoVersion % Test

  //
  //
  //

  def Slf4jGroup = "org.slf4j"
  def Slf4jVersion = "1.7.32"
  lazy val Slf4jApi = Slf4jGroup % "slf4j-api" % Slf4jVersion

  def LogbackGroup = "ch.qos.logback"
  def LogbackVersion = "1.2.5"
  lazy val LogbackCore = LogbackGroup % "logback-core" % LogbackVersion
  lazy val LogbackClassic = LogbackGroup % "logback-classic" % LogbackVersion

  def ScalaLoggingGroup = "com.typesafe.scala-logging"
  def ScalaLoggingVersion = "3.9.4"
  lazy val ScalaLogging = ScalaLoggingGroup %% "scala-logging" % ScalaLoggingVersion

  def CaffeineGroup = "com.github.ben-manes.caffeine"
  def CaffeineVersion = "3.0.4"
  lazy val Caffeine = CaffeineGroup % "caffeine" % CaffeineVersion

  def JsoupVersion = "1.14.2"
  lazy val Jsoup = "org.jsoup" % "jsoup" % JsoupVersion

  def OpenNlpVersion = "1.9.3"
  lazy val OpenNlpTools = "org.apache.opennlp" % "opennlp-tools" % OpenNlpVersion

  def ScoptVersion = "4.0.1"
  lazy val Scopt = "com.github.scopt" %% "scopt" % ScoptVersion

  def SttpZioVersion = "3.3.9"
  lazy val SttpZio = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % SttpZioVersion

  lazy val ZioCache = ZioGroup %% "zio-cache" % "0.1.0"
  lazy val ZioTest = ZioGroup %% "zio-test" % ZioVersion % "test"
  lazy val ZioTestSbt = ZioGroup %% "zio-test-sbt" % ZioVersion % "test"

}

import sbt._

object Dependencies {

  def Scala3Version = "3.0.0"

  // Core

  lazy val CatsCore = "org.typelevel" %% "cats-core" % "2.6.1"

  def CirceGroup = "io.circe"
  def CirceVersion = "0.14.1"
  lazy val CirceCore = CirceGroup %% "circe-core" % CirceVersion
  lazy val CirceGeneric = CirceGroup %% "circe-generic" % CirceVersion
  lazy val CirceParser = CirceGroup %% "circe-parser" % CirceVersion

  lazy val Scalactic = "org.scalactic" %% "scalactic" % "3.2.10"

  lazy val Zio = "dev.zio" %% "zio" % "1.0.12"

  // AWS

  def AwsGroup = "com.amazonaws"
  lazy val AwsLambdaCore = AwsGroup % "aws-lambda-java-core" % "1.2.1"
  lazy val AwsLambdaEvents = AwsGroup % "aws-lambda-java-events" % "3.10.0"
  lazy val AwsSqs = AwsGroup % "amazon-sqs-java-messaging-lib" % "1.0.4"

  // Testing

  lazy val ScalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % Test

  lazy val Mockito = "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test

  //
  //
  //

  def Twitter4jGroup = "org.twitter4j"
  def Twitter4jVersion = "4.0.7"
  lazy val Twitter4j = Twitter4jGroup % "twitter4j-core" % Twitter4jVersion

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

  lazy val SttpZio = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio1" % "3.3.15"

}

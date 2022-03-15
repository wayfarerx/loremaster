import sbt._

object Dependencies {

  def Scala3Version = "3.0.0"

  // Cats

  lazy val CatsCore = "org.typelevel" %% "cats-core" % "2.7.0"

  // Circe

  def CirceGroup = "io.circe"
  def CirceVersion = "0.14.1"
  lazy val CirceCore = CirceGroup %% "circe-core" % CirceVersion
  lazy val CirceGeneric = CirceGroup %% "circe-generic" % CirceVersion
  lazy val CirceParser = CirceGroup %% "circe-parser" % CirceVersion

  // ZIO

  lazy val Zio = "dev.zio" %% "zio" % "1.0.12"

  // AWS

  def AwsGroup = "com.amazonaws"
  lazy val AwsLambdaCore = AwsGroup % "aws-lambda-java-core" % "1.2.1"
  lazy val AwsLambdaEvents = AwsGroup % "aws-lambda-java-events" % "3.10.0"
  lazy val AwsSqs = AwsGroup % "amazon-sqs-java-messaging-lib" % "1.0.4"

  // Testing

  lazy val ScalaTest = "org.scalatest" %% "scalatest" % "3.2.10" % Test

  lazy val Mockito = "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test

}

import sbt._

object Dependencies {

  def Scala3Version = "3.0.0"

  def CirceGroup = "io.circe"
  def CirceVersion = "0.14.1"
  lazy val CirceCore = CirceGroup %% "circe-core" % CirceVersion
  lazy val CirceGeneric = CirceGroup %% "circe-generic" % CirceVersion
  lazy val CirceParser = CirceGroup %% "circe-parser" % CirceVersion

  def ScoptVersion = "4.0.1"
  lazy val Scopt = "com.github.scopt" %% "scopt" % ScoptVersion

  lazy val SttpZio = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % "3.3.9"

  def ZioGroup = "dev.zio"
  def ZioVersion = "1.0.9"
  lazy val Zio = ZioGroup %% "zio" % ZioVersion
  lazy val ZioTest = ZioGroup %% "zio-test" % ZioVersion % "test"
  lazy val ZioTestSbt = ZioGroup %% "zio-test-sbt" % ZioVersion % "test"

}

import sbt._

object Dependencies {

  def Scala3Version = "3.0.0"

  def CaffeineGroup = "com.github.ben-manes.caffeine"
  def CaffeineVersion = "3.0.4"
  lazy val Caffeine = CaffeineGroup % "caffeine" % CaffeineVersion

  def CirceGroup = "io.circe"
  def CirceVersion = "0.14.1"
  lazy val CirceCore = CirceGroup %% "circe-core" % CirceVersion
  lazy val CirceGeneric = CirceGroup %% "circe-generic" % CirceVersion
  lazy val CirceParser = CirceGroup %% "circe-parser" % CirceVersion

  def JsoupVersion = "1.14.2"
  lazy val Jsoup = "org.jsoup" % "jsoup" % JsoupVersion

  def LogbackGroup = "ch.qos.logback"
  def LogbackVersion = "1.2.5"
  lazy val LogbackCore = LogbackGroup % "logback-core" % LogbackVersion
  lazy val LogbackClassic = LogbackGroup % "logback-classic" % LogbackVersion

  def OpenNlpVersion = "1.9.3"
  lazy val OpenNlpTools = "org.apache.opennlp" % "opennlp-tools" % OpenNlpVersion

  def ScoptVersion = "4.0.1"
  lazy val Scopt = "com.github.scopt" %% "scopt" % ScoptVersion

  def Slf4jVersion = "1.7.32"
  lazy val Slf4jApi = "org.slf4j" % "slf4j-api" % Slf4jVersion

  def SttpZioVersion = "3.3.9"
  lazy val SttpZio = "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % SttpZioVersion

  def ZioGroup = "dev.zio"
  def ZioVersion = "1.0.11"
  lazy val Zio = ZioGroup %% "zio" % ZioVersion
  lazy val ZioCache = ZioGroup %% "zio-cache" % "0.1.0"
  lazy val ZioTest = ZioGroup %% "zio-test" % ZioVersion % "test"
  lazy val ZioTestSbt = ZioGroup %% "zio-test-sbt" % ZioVersion % "test"

}

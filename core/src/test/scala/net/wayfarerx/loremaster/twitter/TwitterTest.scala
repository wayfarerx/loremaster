package net.wayfarerx.loremaster
package twitter

import java.io.IOException

import scala.concurrent.duration.*

import zio.{Runtime, UIO, ZIO, console, system}

import model.*

object TwitterTest:

  def main(args: Array[String]): Unit = System exit {
    Runtime.default.unsafeRunTask {
      {
        for
          consumerKey <- require("consumerKey")
          consumerSecret <- require("consumerSecret")
          accessToken <- require("accessToken")
          accessTokenSecret <- require("accessTokenSecret")
          connectionTimeout <- require("connectionTimeout")
          connectionTimeoutDuration <- ZIO(Duration(connectionTimeout).asInstanceOf[FiniteDuration])
          _ <- TwitterClient(
            consumerKey,
            consumerSecret,
            accessToken,
            accessTokenSecret,
            connectionTimeoutDuration
          ).postTweet(Book.of("TEST"))
        yield 0
      } catchAll (report(_))
    }
  }

  private def require(key: String): ZIO[system.System, Exception, String] = for
    variable <- system.env(key)
    result <- variable.fold(ZIO.fail(IllegalStateException(s"Missing environment variable: $key")))(UIO(_))
  yield result

  private def report(thrown: Throwable, cause: Boolean = false): ZIO[console.Console, IOException, Int] = for
    _ <- console.putStrLnErr(
      s"FAILED: ${if cause then "  caused by " else ""}${thrown.getClass.getSimpleName}(${thrown.getMessage})"
    )
    _ <- Option(thrown.getCause).fold(ZIO.unit)(report(_, true))
  yield 1

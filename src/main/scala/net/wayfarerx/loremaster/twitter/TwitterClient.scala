/* TwitterClient.scala
 *
 * Copyright (c) 2022 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package twitter

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.io.IOException

import scala.concurrent.duration.*
import scala.jdk.DurationConverters.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import zio.{Task, UIO, ZIO}

import configuration.*
import event.*
import http.*
import logging.*

/**
 * Definition of a Twitter client.
 */
trait TwitterClient extends (TwitterEvent => Task[Option[(TwitterEvent, FiniteDuration)]])

/**
 * Definitions associated with Twitter clients.
 */
object TwitterClient extends ((TwitterConfiguration, LogFactory) => Task[TwitterClient]) :

  /** The Twitter HTTP endpoint to connect to. */
  private[this] val TweetsEndpoint = "https://api.twitter.com/2/tweets"

  /**
   * Creates an implementation of the Twitter client.
   *
   * @param config     The Twitter configuration to use.
   * @param logFactory The log factory to use.
   * @return An implementation of the Twitter client.
   */
  override def apply(config: TwitterConfiguration, logFactory: LogFactory): Task[TwitterClient] = for
    log <- logFactory.log[TwitterClient]
    _ <- log.debug(s"${TwitterConfiguration.ConnectionTimeout} = ${config.connectionTimeout}")
    _ <- log.debug(s"${TwitterConfiguration.BearerToken} = ${"*" * config.bearerToken.length}")
    _ <- log.debug(s"${TwitterConfiguration.RetryPolicy} = ${config.retryPolicy}")
    http <- Http(config.connectionTimeout)
  yield Live(config.bearerToken, config.retryPolicy getOrElse Retries.Default, log, http)

  /**
   * A live implementation of the Twitter client.
   *
   * @param bearerToken The bearer token to use.
   * @param retries     The retry policy to use.
   * @param log         The log to use.
   * @param http        The HTTP client to use.
   */
  private final class Live(
    bearerToken: String,
    retries: Retries,
    log: Log,
    http: Http
  ) extends TwitterClient :

    /* Post the specified tweet. */
    override def apply(event: TwitterEvent): Task[Option[(TwitterEvent, FiniteDuration)]] = http.post(
      TweetsEndpoint,
      emitJson(TweetsBody(text = Some(event.book.paragraphs.iterator mkString "\r\n\r\n"))),
      "Authorization" -> s"Bearer $bearerToken", "Content-type" -> "application/json"
    ).map(_ => None) catchSome { case problem: Http.Problem if problem.shouldRetry =>
      retries(event).fold(Task.fail(problem))(UIO.some)
    }

  /**
   * The body of the request sent to the tweets endpoint.
   *
   * @param text The optional text of the tweet being created.
   */
  private case class TweetsBody(text: Option[String] = None)

  /**
   * Factory for tweets endpoint request bodies.
   */
  private object TweetsBody extends (Option[String] => TweetsBody) :

    /** The given encoding of tweets bodies to JSON. */
    given Encoder[TweetsBody] = deriveEncoder[TweetsBody].mapJson(_.dropNullValues)

    /** The given decoding of tweets bodies from JSON. */
    given Decoder[TweetsBody] = deriveDecoder
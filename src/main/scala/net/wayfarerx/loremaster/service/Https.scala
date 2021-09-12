/* Https.scala
 *
 * Copyright (c) 2021 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package service

import java.time.Instant
import zio.Task

/**
 * Definition of the HTTPS service API.
 */
trait Https {

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  def get(host: String, location: Location, ifModifiedSince: Option[Instant] = None): Task[Https.Response]

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  inline final def get(host: String, location: Location, ifModifiedSince: Instant): Task[Https.Response] =
    get(host, location, Option(ifModifiedSince))

}

/**
 * Definitions associated with HTTPS services.
 */
object Https:

  import sttp.client3.*
  import zio.{Has, RIO}

  import scala.concurrent.duration.*

  /** The live HTTPS layer. */
  val live: zio.RLayer[Has[Configuration] & Has[Log.Factory], Has[Https]] =
    zio.ZLayer.fromEffect {
      for
        config <- RIO.service[Configuration]
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Https].getSimpleName)
        result <- apply(config.connectionTimeout, log)
      yield result
    }

  /**
   * Creates an HTTPS service that uses the specified configuration.
   *
   * @param connectionTimeout The length of the HTTPS request timeout.
   * @param log               The log to append to.
   * @return An HTTPS service that uses the specified configuration.
   */
  def apply(connectionTimeout: FiniteDuration, log: Log): Task[Https] =
    httpclient.zio.HttpClientZioBackend(SttpBackendOptions.connectionTimeout(connectionTimeout)) map (Live(_, log))

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  inline def get(
    host: String,
    location: Location,
    ifModifiedSince: Option[Instant] = None
  ): RIO[Has[Https], Response] =
    RIO.service flatMap (_.get(host, location, ifModifiedSince))

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  inline def get(
    host: String,
    location: Location,
    ifModifiedSince: Instant
  ): RIO[Has[Https], Response] =
    RIO.service flatMap (_.get(host, location, ifModifiedSince))

  /**
   * The type of response returned from HTTPS operations.
   */
  sealed trait Response

  /**
   * Definitions of the supported HTTPS responses.
   */
  object Response:

    /** The not found response. */
    case object NotFound extends Response

    /** The not modified response. */
    case object NotModified extends Response

    /** The content response. */
    case class Content(text: String) extends Response

  /** The type of backend used by the live implementation. */
  private type LiveBackend = SttpBackend[Task, sttp.capabilities.zio.ZioStreams with sttp.capabilities.WebSockets]

  /**
   * The live HTTPS implementation.
   *
   * @param backend The backend to use.
   * @param log     The log to append to.
   */
  private case class Live(backend: LiveBackend, log: Log) extends Https :

    import Live.*
    import sttp.model.StatusCode

    /* Return the content of the specified resource if it exists. */
    override def get(host: String, location: Location, ifModifiedSince: Option[Instant]): Task[Response] =
      val target = s"https://$host/${location.encoded}"
      for
        request <- ifModifiedSince.fold(pure(basicRequest)) {
          httpTimestamp(_) map (basicRequest.header("If-Modified-Since", _))
        }
        response <- request.get(uri"$target").send(backend)
        result <- response.code match
          case StatusCode.NotFound => pure(Response.NotFound)
          case StatusCode.NotModified => pure(Response.NotModified)
          case _ if response.isSuccess => pure(Response.Content(response.body getOrElse ""))
          case code => fail(s"Unable to download $target: $code ${response.statusText}.")
      yield result

  /**
   * Factory for live https services.
   */
  private object Live extends ((LiveBackend, Log) => Live) :

    import java.time.format.DateTimeFormatter
    import java.time.{ZoneId, ZoneOffset}
    import java.util.Locale

    /** The memoized HTTP timestamp formatter. */
    private val HttpTimestamps = Task {
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId of "GMT")
    }.memoize.flatten

    /**
     * Returns the HTTP timestamp of the specified instant.
     *
     * @param instant The instant to convert.
     * @return The HTTP timestamp of the specified instant.
     */
    inline private def httpTimestamp(instant: Instant): Task[String] = for
      utc <- Task(instant atOffset ZoneOffset.UTC)
      timestamps <- HttpTimestamps
      result <- Task(timestamps format utc)
    yield result
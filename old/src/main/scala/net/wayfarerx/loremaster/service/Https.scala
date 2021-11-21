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

import java.time.{Instant, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

import scala.concurrent.duration.*

import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode

import zio.{Has, RIO, RLayer, Task, ZLayer}

import model.*

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
  def get(host: String, location: Location, ifModifiedSince: Instant): Task[Https.Response] =
    get(host, location, Option(ifModifiedSince))

}

/**
 * Definitions associated with HTTPS services.
 */
object Https:

  /** The live HTTPS layer. */
  val live: RLayer[Has[Configuration] & Has[Log.Factory], Has[Https]] = ZLayer fromEffect {
    for
      config <- RIO.service[Configuration]
      logFactory <- RIO.service[Log.Factory]
      log <- logFactory(classOf[Https].getSimpleName)
      https <-
        val title = config.applicationTitle.replaceAll("\\s+", "")
        val version = config.applicationVersion.replaceAll("\\s+", "")
        apply(config.remoteConnectionTimeout, log, s"$title/$version")
    yield https
  }

  /**
   * Creates an HTTPS service that uses the specified configuration.
   *
   * @param connectionTimeout The length of the HTTPS request timeout.
   * @param log               The log to append to.
   * @param userAgent         The user agent header to provide.
   * @return An HTTPS service that uses the specified configuration.
   */
  def apply(connectionTimeout: FiniteDuration, log: Log, userAgent: String): Task[Https] =
    HttpClientZioBackend(SttpBackendOptions.connectionTimeout(connectionTimeout)) map (Live(_, log, userAgent))

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  inline def get(host: String, location: Location, ifModifiedSince: Option[Instant] = None): RIO[Has[Https], Response] =
    RIO.service flatMap (_.get(host, location, ifModifiedSince))

  /**
   * Returns the content of the specified resource if it exists.
   *
   * @param host            The host to connect to.
   * @param location        The location of the resource to download.
   * @param ifModifiedSince Only download if the resource was modified after the instant.
   * @return The content of the specified resource if it exists.
   */
  inline def get(host: String, location: Location, ifModifiedSince: Instant): RIO[Has[Https], Response] =
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
  private type LiveBackend = SttpBackend[Task, ZioStreams with WebSockets]

  /**
   * The live HTTPS implementation.
   *
   * @param backend   The backend to use.
   * @param log       The log to append to.
   * @param userAgent The user agent header to provide.
   */
  private case class Live(backend: LiveBackend, log: Log, userAgent: String) extends Https :

    /** The template request sent from this service. */
    private val templateRequest = basicRequest.header("User-Agent", userAgent)

    /* Return the content of the specified resource if it exists. */
    override def get(host: String, location: Location, ifModifiedSince: Option[Instant]): Task[Response] =
      val target = s"https://$host/$location"
      for
        httpRequest <- ifModifiedSince.fold(pure(templateRequest)) { instant =>
          for
            utcInstant <- Task(instant atOffset ZoneOffset.UTC)
            httpTimestamps <- Live.HttpTimestamps
            httpInstant <- Task(httpTimestamps format utcInstant)
          yield templateRequest.header("If-Modified-Since", httpInstant)
        }
        httpResponse <- httpRequest.get(uri"$target").send(backend)
        result <- httpResponse.code match
          case StatusCode.NotFound => pure(Response.NotFound)
          case StatusCode.NotModified => pure(Response.NotModified)
          case _ if httpResponse.isSuccess => pure(Response.Content(httpResponse.body getOrElse ""))
          case code => fail(s"Unable to download $target: $code ${httpResponse.statusText}.")
      yield result

  /**
   * Factory for live https services.
   */
  private object Live extends ((LiveBackend, Log, String) => Live) :

    /** The memoized HTTP timestamp formatter. */
    private val HttpTimestamps = Task {
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId of "GMT")
    }.memoize.flatten
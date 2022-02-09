/* Http.scala
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
package http

import java.io.IOException
import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}

import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.DurationConverters.*
import scala.util.control.NoStackTrace

import zio.{Task, UIO}

/**
 * A gateway to HTTP services.
 */
trait Http:

  import Http.Resource

  /**
   * Executes an HTTP HEAD operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the HEAD operation on.
   * @param headers  The headers to submit.
   * @return The content returned from the HTTP HEAD operation.
   */
  def head[T: Resource](resource: T, headers: (String, String)*): Task[Map[String, List[String]]]

  /**
   * Executes an HTTP GET operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the GET operation on.
   * @param headers  The headers to submit.
   * @return The content returned from the HTTP GET operation.
   */
  def get[T: Resource](resource: T, headers: (String, String)*): Task[(Map[String, List[String]], String)]

  /**
   * Executes an HTTP POST operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the POST operation on.
   * @param content  The content to POST.
   * @param headers  The headers to submit.
   * @return The content returned from the HTTP POST operation.
   */
  def post[T: Resource](resource: T, content: String, headers: (String, String)*): Task[Unit]

/**
 * Factory for HTTP gateways.
 */
object Http extends (HttpClient => Http) :

  /** The OK HTTP response code. */
  private[http] val OK = 200

  /** The User-Agent HTTP header. */
  private[http] val UserAgent = "User-Agent"

  /**
   * Creates an HTTP gateway.
   *
   * @param connectTimeout The optional connection timeout to configure.
   * @return A new HTTP gateway.
   */
  def apply(connectTimeout: Option[FiniteDuration] = None): Task[Http] = Task {
    val withVersion = HttpClient.newBuilder.version(HttpClient.Version.HTTP_2)
    val withConnectTimeout = connectTimeout.map(_.toJava).fold(withVersion)(withVersion.connectTimeout)
    withConnectTimeout.build
  }.map(apply)

  /**
   * Creates an HTTP gateway.
   *
   * @param client The client the HTTP gateway should use.
   * @return A new HTTP gateway.
   */
  override def apply(client: HttpClient): Http = Live(client)

  /**
   * Support for HTTP resources.
   *
   * @tparam T The type that represents an HTTP resource.
   */
  trait Resource[T] extends (T => Task[URI])

  /**
   * Factory for HTTP resource support.
   */
  object Resource:

    /** The given resource support for strings. */
    given Resource[String] = str => Task(URI.create(str))

    /** The given resource support for URIs. */
    given Resource[URI] = UIO(_)

    /**
     * Returns the specified resource support.
     *
     * @tparam T The type that represents an HTTP resource.
     * @return The specified resource support.
     */
    def apply[T: Resource]: Resource[T] = summon[Resource[T]]

  /**
   * A problem encountered by the HTTP system.
   *
   * @param message     The message that describes the problem.
   * @param statusCode  The HTTP status code that was returned.
   * @param shouldRetry True if the request should be retried, defaults to false.
   */
  final class Problem(
    val message: String,
    val statusCode: Option[Int],
    val shouldRetry: Boolean
  ) extends RuntimeException(message) with NoStackTrace

  /**
   * The live HTTP gateway implementation.
   *
   * @param client The HTTP client to use.
   */
  private final class Live(client: HttpClient) extends Http :

    /* Execute an HTTP HEAD operation. */
    override def head[T: Resource](resource: T, headers: (String, String)*) = http(
      _.method("HEAD", HttpRequest.BodyPublishers.noBody),
      resource,
      headers,
      HttpResponse.BodyHandlers.discarding
    ).map(_._1)

    /* Execute an HTTP GET operation. */
    override def get[T: Resource](resource: T, headers: (String, String)*) = http(
      _.GET,
      resource,
      headers,
      HttpResponse.BodyHandlers.ofString
    )

    /* Execute an HTTP POST operation. */
    override def post[T: Resource](resource: T, content: String, headers: (String, String)*) = http(
      _.POST(HttpRequest.BodyPublishers.ofString(content)),
      resource,
      headers,
      HttpResponse.BodyHandlers.discarding
    ).map(_ => ())

    /**
     * Executes an HTTP operation.
     *
     * @tparam I The type of resource to operate on.
     * @tparam O The type of output to produce.
     * @param method The method definition to use.
     * @param resource The resource to operate on.
     * @param headers The headers to send.
     * @param bodyHandler The body handler to use.
     * @return The result of executing an HTTP operation.
     */
    private[this] def http[I: Resource, O](
      method: HttpRequest.Builder => HttpRequest.Builder,
      resource: I,
      headers: Seq[(String, String)],
      bodyHandler: HttpResponse.BodyHandler[O]
    ): Task[(Map[String, List[String]], O)] = for
      uri <- Resource[I].apply(resource)
      builder <- Task {
        ((UserAgent -> Loremaster) +: headers)
          .foldLeft(method(HttpRequest.newBuilder(uri)))((builder, h) => builder.header(h._1, h._2))
      }
      request <- Task(builder.build)
      response <- Task(client.send(request, bodyHandler)) catchSome {
        case ioe: IOException =>
          Task.fail(Problem(s"${request.method} $resource: ${ioe.getMessage}", None, true))
      }
      result <- response.statusCode match
        case OK =>
          UIO(response.body)
        case _statusCode =>
          Task.fail(Problem(
            s"${request.method} $resource => $_statusCode",
            Some(_statusCode),
            _statusCode / 100 >= 5
          ))
    yield response.headers.map.asScala.view.mapValues(_.asScala.toList).toMap -> result
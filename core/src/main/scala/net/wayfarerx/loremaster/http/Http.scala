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
import scala.util.control.{NoStackTrace, NonFatal}

import zio.{IO, Task, UIO}

/**
 * A gateway to HTTP services.
 */
trait Http:

  import Http.*

  /**
   * Executes an HTTP HEAD operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the HEAD operation on.
   * @param headers  The headers to submit.
   * @return The headers returned from the HTTP HEAD operation.
   */
  def head[T: Resource](resource: T, headers: (String, String)*): Result[Headers]

  /**
   * Executes an HTTP GET operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the GET operation on.
   * @param headers  The headers to submit.
   * @return The headers and content returned from the HTTP GET operation.
   */
  def get[T: Resource](resource: T, headers: (String, String)*): Result[(Headers, String)]

  /**
   * Executes an HTTP POST operation.
   *
   * @tparam T The type that represents an HTTP resource.
   * @param resource The resource to execute the POST operation on.
   * @param content  The content to POST.
   * @param headers  The headers to submit.
   * @return The headers returned from the HTTP POST operation.
   */
  def post[T: Resource](resource: T, content: String, headers: (String, String)*): Result[Headers]

/**
 * Factory for HTTP gateways.
 */
object Http extends (HttpClient => Http) :

  /** The data type used to represent collections of HTTP headers. */
  type Headers = Map[String, List[String]]

  /** The type of result returned by HTTP services. */
  type Result[T] = IO[HttpProblem, T]

  /** The headers to include in every HTTP request. */
  private[http] val CommonHeaders: Map[String, String] = Map("User-Agent" -> Application)

  /**
   * Creates an HTTP gateway.
   *
   * @param connectionTimeout The optional connection timeout to enforce.
   * @return A new HTTP gateway.
   */
  def apply(connectionTimeout: Option[FiniteDuration] = None): Task[Http] = Task {
    val withVersion = HttpClient.newBuilder.version(HttpClient.Version.HTTP_2)
    val withConnectTimeout = connectionTimeout.fold(withVersion)(withVersion connectTimeout _.toJava)
    withConnectTimeout.build
  }.map(apply)

  /**
   * Creates an HTTP gateway.
   *
   * @param connectionTimeout The connection timeout to enforce.
   * @return A new HTTP gateway.
   */
  def apply(connectionTimeout: FiniteDuration): Task[Http] = apply(Option(connectionTimeout))

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
   * The live HTTP gateway implementation.
   *
   * @param client The HTTP client to use.
   */
  private final class Live(client: HttpClient) extends Http :

    /* Execute an HTTP HEAD operation. */
    override def head[T: Resource](resource: T, headers: (String, String)*) = apply(
      resource,
      _.method("HEAD", HttpRequest.BodyPublishers.noBody),
      headers,
      HttpResponse.BodyHandlers.discarding
    ).map(_._1)

    /* Execute an HTTP GET operation. */
    override def get[T: Resource](resource: T, headers: (String, String)*) = apply(
      resource,
      _.GET,
      headers,
      HttpResponse.BodyHandlers.ofString
    )

    /* Execute an HTTP POST operation. */
    override def post[T: Resource](resource: T, content: String, headers: (String, String)*) = apply(
      resource,
      _.POST(HttpRequest.BodyPublishers.ofString(content)),
      headers,
      HttpResponse.BodyHandlers.discarding
    ).map(_._1)

    /**
     * Executes an HTTP operation.
     *
     * @tparam I The type of resource to operate on.
     * @tparam O The type of output to produce.
     * @param resource    The resource to operate on.
     * @param method      The method definition to use.
     * @param headers     The headers to send.
     * @param bodyHandler The body handler to use.
     * @return The result of executing an HTTP operation.
     */
    private def apply[I: Resource, O](
      resource: I,
      method: HttpRequest.Builder => HttpRequest.Builder,
      headers: Seq[(String, String)],
      bodyHandler: HttpResponse.BodyHandler[O]
    ): Result[(Headers, O)] = {
      for
        uri <- Resource[I].apply(resource)
        builder <- Task(method(HttpRequest.newBuilder(uri)))
        request <- Task {
          (CommonHeaders ++ headers).foldLeft(builder) {
            case (builder, (name, value)) => builder.header(name, value)
          }.build
        }
        response <- Task(client.send(request, bodyHandler)) catchSome {
          case thrown: IOException => Task fail {
            HttpProblem(Messages.transportFailure(request.method, uri, thrown), Some(thrown), true)
          }
        }
        result <- response.statusCode match
          case 200 => UIO(response.body)
          case statusCode => Task fail {
            HttpProblem(Messages.problematicResponse(request.method, uri, statusCode), shouldRetry = statusCode >= 500)
          }
      yield response.headers.map.asScala.view.mapValues(_.asScala.toList).toMap -> result
    } catchAll {
      case problem: HttpProblem => IO.fail(problem)
      case NonFatal(thrown) => IO.fail(HttpProblem(Messages.unexpectedFailure(resource, thrown), Some(thrown)))
      case fatal => IO.die(fatal)
    }
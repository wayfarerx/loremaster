/* HttpTest.scala
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
import java.net.http.{HttpClient, HttpHeaders, HttpRequest, HttpResponse}
import java.util.Optional
import javax.net.ssl.SSLSession

import scala.jdk.CollectionConverters.*

import zio.{Runtime, Task}

import org.scalatest.*
import flatspec.*
import matchers.*

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for the HTTP support.
 */
class HttpTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  /** Test data. */
  private[this] val data = "TEST"

  /** A test resource. */
  private[this] val resource = "https://wayfarerx.net/"

  "HTTP support" should "execute HEAD requests" in {
    Runtime.default.unsafeRunTask {
      for
        http <- emptyResponses(Http.OK)
        _ <- http.head(resource)
      yield ()
    }
  }

  it.should ("execute GET requests") in {
    Runtime.default.unsafeRunTask {
      for
        http <- nonEmptyResponses(Http.OK, data)
        result <- http.get(resource)
      yield result._2
    } shouldBe data
  }

  it.should ("execute POST requests") in {
    Runtime.default.unsafeRunTask {
      for
        http <- emptyResponses(Http.OK)
        result <- http.post(resource, data)
      yield ()
    }
  }

  it.should ("handle IO exceptions") in {
    val httpClient = mock[HttpClient]
    val http = Http(httpClient)
    when(httpClient.send(any(), any())).thenThrow(IOException("ERROR"))
    Runtime.default.unsafeRunTask {
      http.head(resource) catchSome {
        case problem: Http.Problem => Task {
          problem.message.isEmpty shouldBe false
          problem.statusCode shouldBe None
          problem.shouldRetry shouldBe true
        }
      }
    }
  }

  it.should ("handle status codes that cannot be retried") in {
    Runtime.default.unsafeRunTask {
      for
        http <- emptyResponses(404)
        _ <- http.head(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message.isEmpty shouldBe false
            problem.statusCode shouldBe Some(404)
            problem.shouldRetry shouldBe false
          }
        }
      yield ()
    }
  }

  it.should ("handle status codes that can be retried") in {
    Runtime.default.unsafeRunTask {
      for
        http <- nonEmptyResponses(500, data)
        _ <- http.get(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message.isEmpty shouldBe false
            problem.statusCode shouldBe Some(500)
            problem.shouldRetry shouldBe true
          }
        }
      yield ()
    }
  }

  it.should ("support creating from a configuration") in {
    Runtime.default.unsafeRunTask(Http(None)) shouldBe an [Http]
  }

  it.should ("support URI resources") in {
    Http.Resource[URI].apply(URI.create(resource))
  }

  /** Creates a test HTTP support that returns empty responses. */
  private[this] def emptyResponses(statusCode: Int): Task[Http] =
    val httpClient = mock[HttpClient]
    val http = Http(httpClient)
    for
      uri <- Http.Resource[String].apply(resource)
      _ <- Task {
        doAnswer { invocation =>
          val request = invocation.getArguments.apply(0).asInstanceOf[HttpRequest]
          validateRequest(uri, request)
          TestResponse.Empty(uri, statusCode, request)
        }.when(httpClient).send(any(), any())
      }
    yield http

  /** Creates a test HTTP support that returns non-empty responses. */
  private[this] def nonEmptyResponses[T](statusCode: Int, body: T): Task[Http] =
    val httpClient = mock[HttpClient]
    val http = Http(httpClient)
    for
      uri <- Http.Resource[String].apply(resource)
      _ <- Task {
        doAnswer { invocation =>
          val request = invocation.getArguments.apply(0).asInstanceOf[HttpRequest]
          validateRequest(uri, request)
          TestResponse.NonEmpty(uri, statusCode, request, body)
        }.when(httpClient).send(any(), any())
      }
    yield http

  /** Validates that a request is what we expect. */
  private[this] def validateRequest(uri: URI, request: HttpRequest): Unit =
    request.uri shouldBe uri
    request.headers.map.asScala.map {
      case (k, v) => k -> v.asScala
    } shouldBe Map(Http.UserAgent -> List(Loremaster))

  /**
   * Base class for test responses.
   *
   * @tparam T The type of data to return.
   */
  private[this] sealed trait TestResponse[T] extends HttpResponse[T] :

    final override def previousResponse: Optional[HttpResponse[T]] = Optional.empty

    final override def headers: HttpHeaders = HttpHeaders.of(Map.empty.asJava, (_, _) => true)

    final override def sslSession: Optional[SSLSession] = Optional.empty

    final override def version: HttpClient.Version = HttpClient.Version.HTTP_2

  /**
   * Definitions of the supported test responses.
   */
  private[this] object TestResponse:

    /** A test response with no body. */
    case class Empty(
      uri: URI,
      statusCode: Int,
      request: HttpRequest
    ) extends TestResponse[Void] :

      override def body: Void = null

    /** A test response with a body. */
    case class NonEmpty[T](
      uri: URI,
      statusCode: Int,
      request: HttpRequest,
      body: T
    ) extends TestResponse[T]
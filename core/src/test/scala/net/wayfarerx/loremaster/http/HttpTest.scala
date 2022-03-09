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
        httpClient <- mockEmptyHttpClient("HEAD", 200)
        _ <- Http(httpClient).head(resource)
      yield ()
    }
  }

  it.should("execute GET requests") in {
    Runtime.default.unsafeRunTask {
      for
        httpClient <- mockHttpClient("GET", 200, data)
        result <- Http(httpClient).get(resource)
      yield result._2
    } shouldBe data
  }

  it.should("execute POST requests") in {
    Runtime.default.unsafeRunTask {
      for
        httpClient <- mockEmptyHttpClient("POST", 200)
        _ <- Http(httpClient).post(resource, data)
      yield ()
    }
  }

  it.should("handle IO exceptions") in {
    val httpClient = mock[HttpClient]
    val ioException = IOException("ERROR")
    when(httpClient.send(any(), any())).thenThrow(ioException)
    Runtime.default.unsafeRunTask {
      for
        uri <- Http.Resource[String].apply(resource)
        _ <- Http(httpClient).head(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message shouldBe Messages.transportFailure("HEAD", uri, ioException)
            problem.thrown shouldBe Some(ioException)
            problem.shouldRetry shouldBe true
          }
        }
      yield ()
    }
  }

  it.should("handle unexpected non-fatal exceptions") in {
    val httpClient = mock[HttpClient]
    val illegalArgumentException = IllegalArgumentException("ERROR")
    when(httpClient.send(any(), any())).thenThrow(illegalArgumentException)
    Runtime.default.unsafeRunTask {
      for
        uri <- Http.Resource[String].apply(resource)
        _ <- Http(httpClient).head(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message shouldBe Messages.unexpectedFailure(uri, illegalArgumentException)
            problem.thrown shouldBe Some(illegalArgumentException)
            problem.shouldRetry shouldBe false
          }
        }
      yield ()
    }
  }

  it.should("propagate unexpected fatal exceptions") in {
    val httpClient = mock[HttpClient]
    val interruptedException = InterruptedException()
    when(httpClient.send(any(), any())).thenThrow(interruptedException)
    assert {
      intercept[InterruptedException] {
        Runtime.default.unsafeRunTask {
          for
            uri <- Http.Resource[String].apply(resource)
            _ <- Http(httpClient).head(resource)
          yield ()
        }
      } == interruptedException
    }
  }

  it.should("handle status codes that cannot be retried") in {
    Runtime.default.unsafeRunTask {
      for
        uri <- Http.Resource[String].apply(resource)
        httpClient <- mockEmptyHttpClient("HEAD", 404)
        _ <- Http(httpClient).head(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message shouldBe Messages.problematicResponse("HEAD", uri, 404)
            problem.thrown shouldBe None
            problem.shouldRetry shouldBe false
          }
        }
      yield ()
    }
  }

  it.should("handle status codes that can be retried") in {
    Runtime.default.unsafeRunTask {
      for
        uri <- Http.Resource[String].apply(resource)
        httpClient <- mockEmptyHttpClient("HEAD", 500)
        _ <- Http(httpClient).head(resource) catchSome {
          case problem: Http.Problem => Task {
            problem.message shouldBe Messages.problematicResponse("HEAD", uri, 500)
            problem.thrown shouldBe None
            problem.shouldRetry shouldBe true
          }
        }
      yield ()
    }
  }

  it.should("support creating from a configuration") in {
    Runtime.default.unsafeRunTask(Http(None)) shouldBe an[Http]
  }

  it.should("support URI resources") in {
    Http.Resource[URI].apply(URI.create(resource))
  }

  /**
   * Creates a test HTTP client that returns non-empty responses.
   *
   * @tparam T The type of response returned from the HTTP client.
   * @param method     The HTTP method to expect.
   * @param statusCode The status code returned by the HTTP client.
   * @param body       the body to return from the HTTP client.
   * @return A test HTTP client that returns non-empty responses.
   */
  private def mockHttpClient[T](method: String, statusCode: Int, body: T): Task[HttpClient] =
    val httpClient = mock[HttpClient]
    for
      uri <- Http.Resource[String].apply(resource)
      _ <- Task {
        doAnswer { invocation =>
          val request = invocation.getArguments.apply(0).asInstanceOf[HttpRequest]
          request.method.toUpperCase shouldBe method.toUpperCase
          request.uri shouldBe uri
          request.headers.map.asScala.map {
            case (k, v) => k -> v.asScala.toList
          } shouldBe Http.CommonHeaders.map { case (k, v) => k -> List(v) }
          TestResponse(uri, statusCode, request, body)
        }.when(httpClient).send(any(), any())
      }
    yield httpClient

  /**
   * Creates a test HTTP client that returns empty responses.
   *
   * @param method     The HTTP method to expect.
   * @param statusCode The status code returned by the HTTP client.
   * @return A test HTTP client that returns empty responses.
   */
  private def mockEmptyHttpClient(method: String, statusCode: Int): Task[HttpClient] =
    mockHttpClient[Void](method, statusCode, null)

  /**
   * Base class for test responses.
   *
   * @tparam T The type of data to return.
   */
  private case class TestResponse[T](
    uri: URI,
    statusCode: Int,
    request: HttpRequest,
    body: T
  ) extends HttpResponse[T] :

    /* No previous intermediate response. */
    override def previousResponse: Optional[HttpResponse[T]] = Optional.empty

    /* No response headers. */
    override def headers: HttpHeaders = HttpHeaders.of(Map.empty.asJava, (_, _) => true)

    /* No SSL session. */
    override def sslSession: Optional[SSLSession] = Optional.empty

    /* HTTP 2. */
    override def version: HttpClient.Version = HttpClient.Version.HTTP_2
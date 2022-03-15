/* TwitterClientTest.scala
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
package twitter

import zio.{IO, Runtime}

import http.*
import model.*

import org.scalatest.*
import flatspec.*
import matchers.*

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for twitter clients.
 */
class TwitterClientTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  /** A test bearer. */
  private val testBearer = "BEARER"

  /** A test book. */
  private val testBook = Book.of("A", "B")

  "TitterClient" should "post tweets" in {
    val http = mock[Http]
    when(http.post(
      TwitterClient.TweetsEndpoint,
      emitJson(TwitterClient.Body(text = Some(testBook.toString))),
      "Authorization" -> s"Bearer $testBearer",
      "Content-Type" -> "application/json"
    )) thenReturn IO(Map.empty)
    val client = TwitterClient(testBearer, http)
    Runtime.default.unsafeRunTask(client.postTweet(testBook)) shouldBe ()
  }
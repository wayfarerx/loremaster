/* TwitterClientTest.scala
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

import twitter4j.Twitter

import zio.{IO, Runtime}

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
    val mockStatus = mock[twitter4j.Status]
    val mockTwitter = mock[Twitter]
    when(mockTwitter.updateStatus(testBook.toString)) thenReturn mockStatus
    Runtime.default.unsafeRunTask(TwitterClient(mockTwitter).postTweet(testBook)) shouldBe ()
  }
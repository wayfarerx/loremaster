/* TweetEventTest.scala
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

import java.time.Instant

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import event.*
import model.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for tweet events.
 */
class TwitterEventTest extends AnyFlatSpec with should.Matchers :

  /** A test instant. */
  private val now = Instant.now

  /** A test tweet event. */
  private val test = TwitterEvent(Book.of("A", "B"), now)

  "TwitterEvent" should "encode tweet events to JSON" in {
    Encoder[TwitterEvent].apply(test) shouldBe Json.obj(
      "book" -> test.book.asJson,
      "createdAt" -> now.asJson
    )
  }

  it.should("decode tweet events from JSON") in {
    Decoder[TwitterEvent].apply(HCursor.fromJson(test.asJson)) shouldBe Right(test)
  }

  it.should("Support the event type class") in {
    val evt = Event[TwitterEvent]
    evt.createdAt(test) shouldBe test.createdAt
    evt.previousAttempts(test) shouldBe 0
    val next = evt.nextAttempt(test)
    evt.createdAt(next) shouldBe test.createdAt
    evt.previousAttempts(next) shouldBe 1
  }


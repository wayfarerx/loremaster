/* BookTest.scala
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
package event

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import model.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the tweet event type.
 */
class TweetEventTest extends AnyFlatSpec with should.Matchers :

  /** A test tweet event. */
  private val test = TweetEvent(Book.of("A", "B"))

  "TweetEvent" should "encode tweet events to JSON" in {
    Encoder[TweetEvent].apply(test) shouldBe Json.obj("book" -> test.book.asJson)
  }

  it should "decode tweet events from JSON" in {
    Decoder[TweetEvent].apply(HCursor fromJson test.asJson) shouldBe Right(test)
  }


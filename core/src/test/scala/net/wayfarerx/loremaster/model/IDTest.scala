/* IDTest.scala
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
package model

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the ID type.
 */
class IDTest extends AnyFlatSpec with should.Matchers:

  /** A test ID. */
  private val test = id"test"

  "An ID" should "return its value when converted to a string" in {
    test.toString shouldBe "test"
  }

  "IDs" should "encode IDs to JSON" in {
    Encoder[ID].apply(test) shouldBe Json.fromString(test.toString)
  }

  it should "decode IDs from JSON" in {
    Decoder[ID].apply(HCursor fromJson test.asJson) shouldBe Right(test)
  }

  it should "fail to decode from invalid ID strings" in {
    ID decode "" shouldBe None
    ID decode "." shouldBe None
    ID decode ".." shouldBe None
    ID decode "/" shouldBe None
    ID decode "a\\b" shouldBe None
  }
/* SentenceTest.scala
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
package model

import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the sentence type.
 */
class SentenceTest extends AnyFlatSpec with should.Matchers :

  /** A test token. */
  private val tokenA = Token.Text("TEXT", "noun")

  /** Another test token. */
  private val tokenB = Token.Name("NAME", Token.Name.Category.Person)

  /** A test sentence. */
  private val test = Sentence(NonEmptyList.of(tokenA, tokenB))

  "Sentence" should "encode sentences to JSON" in {
    Encoder[Sentence].apply(test) shouldBe Json.obj(
      "tokens" -> Json.arr(test.tokens.iterator.map(Encoder[Token].apply).toSeq *)
    )
  }

  it.should("decode sentences from JSON") in {
    Decoder[Sentence].apply(HCursor.fromJson(test.asJson)) shouldBe Right(test)
  }

  it.should("construct sentences from tokens") in {
    Sentence.of(tokenA, tokenB) shouldBe test
    Sentence.from(Seq(tokenA, tokenB)) shouldBe Some(test)
  }
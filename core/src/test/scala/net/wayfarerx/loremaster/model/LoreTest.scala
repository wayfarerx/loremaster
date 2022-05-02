/* LoreTest.scala
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
 * Test case for the lore type.
 */
class LoreTest extends AnyFlatSpec with should.Matchers :

  /** A test paragraph. */
  private val paragraphA = Paragraph.of(Sentence.of(Token.Text("TEXT", "noun")))

  /** Another test paragraph. */
  private val paragraphB = Paragraph.of(Sentence.of(Token.Name("NAME", Token.Name.Category.Person)))

  /** A test lore. */
  private val test = Lore(NonEmptyList.of(paragraphA, paragraphB))

  "Lore" should "encode lore to JSON" in {
    Encoder[Lore].apply(test) shouldBe Json.obj(
      "paragraphs" -> Json.arr(test.paragraphs.iterator.map(Encoder[Paragraph].apply).toSeq *)
    )
  }

  it.should("decode lore from JSON") in {
    Decoder[Lore].apply(HCursor.fromJson(test.asJson)) shouldBe Right(test)
  }

  it.should("construct lore from paragraphs") in {
    Lore.of(paragraphA, paragraphB) shouldBe test
    Lore.from(Seq(paragraphA, paragraphB)) shouldBe Some(test)
  }
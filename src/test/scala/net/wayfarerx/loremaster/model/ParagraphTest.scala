/* ParagraphTest.scala
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

import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the paragraph type.
 */
class ParagraphTest extends AnyFlatSpec with should.Matchers :

  /** A test sentence. */
  private val sentenceA = Sentence.of(Token.Text("TEXT", "noun"))

  /** Another test sentence. */
  private val sentenceB = Sentence.of(Token.Name("NAME", Token.Name.Category.Person))

  /** A test paragraph. */
  private val test = Paragraph(NonEmptyList.of(sentenceA, sentenceB))

  "Paragraph" should "encode paragraphs to JSON" in {
    Encoder[Paragraph].apply(test) shouldBe Json.obj(
      "sentences" -> Json.arr(test.sentences.iterator.map(Encoder[Sentence].apply).toSeq *)
    )
  }

  it should "decode paragraphs from JSON" in {
    Decoder[Paragraph].apply(HCursor fromJson test.asJson) shouldBe Right(test)
  }

  it should "construct paragraphs from sentences" in {
    Paragraph.of(sentenceA, sentenceB) shouldBe test
    Paragraph.from(Seq(sentenceA, sentenceB)) shouldBe Some(test)
  }
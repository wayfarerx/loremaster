/* TokenTest.scala
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

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax.*

import org.scalatest.*
import flatspec.*
import matchers.*

import Token.{Name, Text}
import Name.Category

/**
 * Test case for the token type.
 */
class TokenTest extends AnyFlatSpec with should.Matchers :

  /** A test text token. */
  private val testText = Text("TEXT", "noun")

  /** A test name token. */
  private val testName = Name("NAME", Category.Person)

  "Token" should "sort tokens by content and type" in {
    val catHead = Category.values.head
    val catLast = Category.values.last
    Ordering[Token].compare(Text("a", "n"), Text("a", "n")) shouldBe 0
    Ordering[Token].compare(Text("a", "n"), Text("a", "v")) should be < 0
    Ordering[Token].compare(Text("a", "v"), Text("a", "n")) should be > 0
    Ordering[Token].compare(Text("a", "n"), Text("b", "n")) should be < 0
    Ordering[Token].compare(Text("b", "n"), Text("a", "n")) should be > 0
    Ordering[Category].compare(catHead, catHead) shouldBe 0
    Ordering[Category].compare(catHead, catLast) should be < 0
    Ordering[Category].compare(catLast, catHead) should be > 0
    Ordering[Token].compare(Name("a", catHead), Name("a", catHead)) shouldBe 0
    Ordering[Token].compare(Name("a", catHead), Name("a", catLast)) should be < 0
    Ordering[Token].compare(Name("a", catLast), Name("a", catHead)) should be > 0
    Ordering[Token].compare(Name("a", catHead), Name("b", catHead)) should be < 0
    Ordering[Token].compare(Name("b", catHead), Name("a", catHead)) should be > 0
    Ordering[Token].compare(Text("a", "n"), Name("a", catHead)) should be < 0
    Ordering[Token].compare(Text("a", "n"), Name("b", catHead)) should be < 0
    Ordering[Token].compare(Text("b", "n"), Name("a", catHead)) should be > 0
    Ordering[Token].compare(Name("a", catHead), Text("a", "n")) should be > 0
    Ordering[Token].compare(Name("a", catHead), Text("b", "n")) should be < 0
    Ordering[Token].compare(Name("b", catHead), Text("a", "n")) should be > 0
  }

  it.should("encode tokens to JSON") in {
    Encoder[Text].apply(testText) shouldBe Json.obj(
      "text" -> Json.fromString(testText.text),
      "pos" -> Json.fromString(testText.pos)
    )
    Encoder[Token].apply(testText) shouldBe Encoder[Text].apply(testText)
    Encoder[Category].apply(testName.category) shouldBe Json.fromString(testName.category.toString)
    Encoder[Name].apply(testName) shouldBe Json.obj(
      "name" -> Json.fromString(testName.name),
      "category" -> Encoder[Category].apply(testName.category)
    )
    Encoder[Token].apply(testName) shouldBe Encoder[Name].apply(testName)
  }

  it.should("decode tokens from JSON") in {
    Decoder[Text].apply(HCursor.fromJson(testText.asJson)) shouldBe Right(testText)
    Decoder[Token].apply(HCursor.fromJson(testText.asJson)) shouldBe Right(testText)
    Decoder[Category].apply(HCursor.fromJson(testName.category.asJson)) shouldBe Right(testName.category)
    Decoder[Name].apply(HCursor.fromJson(testName.asJson)) shouldBe Right(testName)
    Decoder[Token].apply(HCursor.fromJson(testName.asJson)) shouldBe Right(testName)
  }
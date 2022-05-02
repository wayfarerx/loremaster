/* BookTest.scala
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
 * Test case for the book type.
 */
class BookTest extends AnyFlatSpec with should.Matchers :

  /** A test book. */
  private val test = Book(NonEmptyList.of("A", "B"))

  "A book" should "return its contents when converted to a string" in {
    test.toString shouldBe s"A\r\n\r\nB"
  }

  "Book" should "encode books to JSON" in {
    Encoder[Book].apply(test) shouldBe Json.obj(
      "paragraphs" -> Json.arr(test.paragraphs.iterator.map(Json.fromString).toSeq *)
    )
  }

  it.should("decode books from JSON") in {
    Decoder[Book].apply(HCursor.fromJson(test.asJson)) shouldBe Right(test)
  }

  it.should("construct books from paragraphs") in {
    Book.of("A", "B") shouldBe test
    Book.from(Seq("A", "B")) shouldBe Some(test)
  }
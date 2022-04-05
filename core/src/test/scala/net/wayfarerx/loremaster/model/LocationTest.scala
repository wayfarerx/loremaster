/* LocationTest.scala
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

/**
 * Test case for the location type.
 */
class LocationTest extends AnyFlatSpec with should.Matchers :

  /** The head ID. */
  private val head = id"test"

  /** The last ID. */
  private val last = id"location"

  /** An other ID. */
  private val other = id"other"

  /** A test location. */
  private val test = location"$head/$last"

  "A location" should "provide access to the path it contains" in {
    test.size shouldBe 2
    test.head shouldBe head
    test.tail shouldBe last :: Nil
    test.init shouldBe head :: Nil
    test.last shouldBe last
    test.reverse shouldBe location"$last/$head"
  }

  it.should("support prepend and append operations") in {
    test :+ other shouldBe location"$test/$other"
    other +: test shouldBe location"$other/$test"
    test :++ Location.of(other) shouldBe location"$test/$other"
    Location.of(other) ++: test shouldBe location"$other/$test"
    test.withSuffix("/") shouldBe None
    test.withSuffix(".json") shouldBe Some(location"$test.json")
  }

  it.should("return its encoded value when converted to a string") in {
    test.toString shouldBe s"$head/$last"
  }

  "Locations" should "sort locations as collections of IDs" in {
    Ordering[Location].compare(test, Location.of(head)) shouldBe 1
    Ordering[Location].compare(test, test :+ other) shouldBe -1
  }

  it.should("encode locations to JSON") in {
    Encoder[Location].apply(test) shouldBe Json.fromString(test.toString)
  }

  it.should("decode locations from JSON") in {
    Decoder[Location].apply(HCursor.fromJson(test.asJson)) shouldBe Right(test)
  }

  it.should("decode location strings with single and double dot path elements") in {
    Location.decode(s"$test/./$other") shouldBe Some(test :+ other)
    Location.decode(s"$test/../$other") shouldBe Some(Location.of(head, other))
  }

  it.should("fail to decode from invalid location strings") in {
    Location.decode("") shouldBe None
    Location.decode(".") shouldBe None
    Location.decode("..") shouldBe None
    Location.decode("...") shouldBe None
    Location.decode("/") shouldBe None
    Location.decode("/\\") shouldBe None
  }
/* ConfigurationTest.scala
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
package configuration

import zio.{Runtime, UIO}

import model.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the configuration service.
 */
class ConfigurationTest extends AnyFlatSpec with should.Matchers :

  "A configuration" should "return data from a source" in {
    val config = Configuration {
      case "boolean" => UIO(Some("true"))
      case "byte" => UIO(Some("1"))
      case "short" => UIO(Some("2"))
      case "int" => UIO(Some("3"))
      case "long" => UIO(Some("4"))
      case "float" => UIO(Some("5.1"))
      case "double" => UIO(Some("6.2"))
      case "char" => UIO(Some("a"))
      case "string" => UIO(Some("str"))
      case "id" => UIO(Some("id"))
      case "location" => UIO(Some("the/place"))
      case _ => UIO(None)
    }
    val effect = for
      boolean <- config[Boolean]("boolean")
      byte <- config[Byte]("byte")
      short <- config[Short]("short")
      int <- config[Int]("int")
      long <- config[Long]("long")
      float <- config[Float]("float")
      double <- config[Double]("double")
      char <- config[Char]("char")
      string <- config[String]("string")
      id <- config[ID]("id")
      location <- config[Location]("location")
    yield boolean :: byte :: short :: int :: long :: float :: double :: char :: string :: id :: location :: Nil
    Runtime.default unsafeRunTask effect shouldBe
      true :: 1.toByte :: 2.toShort :: 3 :: 4L :: 5.1f :: 6.2 :: 'a' :: "str" :: id"id" :: location"the/place" :: Nil
  }

  it should "fail to return bad data" in {
    assertThrows[ConfigurationException](Runtime.default unsafeRunTask Configuration(_ => UIO(None))[Int]("int"))
  }
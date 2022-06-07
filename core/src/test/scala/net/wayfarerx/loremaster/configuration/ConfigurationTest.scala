/* ConfigurationTest.scala
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
package configuration

import scala.concurrent.duration.*

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
    val config = new Configuration :
      override def get[T: Configuration.Data](key: String): ConfigurationEffect[Option[T]] = {
        key match
          case "boolean" => UIO.some("true")
          case "byte" => UIO.some("1")
          case "short" => UIO.some("2")
          case "int" => UIO.some("3")
          case "long" => UIO.some("4")
          case "float" => UIO.some("5.1")
          case "double" => UIO.some("6.2")
          case "char" => UIO.some("a")
          case "string" => UIO.some("str")
          case "duration" => UIO.some("3min")
          case "id" => UIO.some("id")
          case "location" => UIO.some("the/location")
          case _ => UIO.none
      } map (_ flatMap Configuration.Data[T])
    Runtime.default.unsafeRunTask {
      for
        boolean <- config[Boolean]("boolean")
        byte <- config[Byte]("byte")
        short <- config[Short]("short")
        int <- config[Int]("int")
        long <- config[Long]("long")
        float <- config[Float]("float")
        double <- config[Double]("double")
        char <- config[Char]("char")
        string <- config[String]("string")
        duration <- config[FiniteDuration]("duration")
        id <- config[ID]("id")
        location <- config[Location]("location")
      yield boolean ::
        byte ::
        short ::
        int ::
        long ::
        float ::
        double ::
        char ::
        string ::
        duration ::
        id ::
        location ::
        Nil
    } shouldBe
      true ::
        1.toByte ::
        2.toShort ::
        3 ::
        4L ::
        5.1f ::
        6.2 ::
        'a' ::
        "str" ::
        3.minutes ::
        id"id" ::
        location"the/location" ::
        Nil
  }

  it.should("fail to return missing data") in {
    val config = new Configuration :
      override def get[T: Configuration.Data](key: String): ConfigurationEffect[Option[T]] = UIO.none
    assertThrows[ConfigurationProblem](Runtime.default.unsafeRunTask(config[Int]("int")))
    Runtime.default.unsafeRunTask(config.get[Int]("int")) shouldBe None
    Runtime.default.unsafeRunTask(config.getOrElse("int", 5)) shouldBe 5
  }
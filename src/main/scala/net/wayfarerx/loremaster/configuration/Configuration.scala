/* Configuration.scala
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

import java.lang.{
  Boolean => JBoolean,
  Integer => JInt,
  Long => JLong,
  Float => JFloat,
  Double => JDouble
}

import scala.reflect.ClassTag

import zio.{Task, UIO}

import model.*

/**
 * Definition of the configuration service.
 */
trait Configuration:

  import Configuration.Data

  /**
   * Returns the value of the specified configuration entry.
   *
   * @param key The key of the configuration entry to return.
   * @return The value of the specified configuration entry.
   */
  final def apply[T: ClassTag : Data](key: String): Task[T] = get(key) flatMap {
    _.fold(Task fail IllegalStateException(
      s"Configuration entry $key: ${summon[ClassTag[T]].runtimeClass.getSimpleName} is not defined."
    ))(UIO(_))
  }

  /**
   * Returns the value of the specified configuration entry if it exists.
   *
   * @param key The key of the configuration entry to return.
   * @return The value of the specified configuration entry if it exists.
   */
  def get[T: Data](key: String): Task[Option[T]]

/**
 * Definitions associated with configurations.
 */
object Configuration:

  /** Support for decoding a specific type of configuration data. */
  trait Data[T]:

    /**
     * Decodes configuration data.
     *
     * @param data The configuration data to decode.
     * @return The decoded configuration data.
     */
    def decode(data: String): Option[T]

  /**
   * Definitions of the supported data types.
   */
  object Data:

    /** False configuration data. */
    private[this] val falses = Set("0", "f", "n", "off", "false", "no")

    /** True configuration data. */
    private[this] val trues = Set("1", "t", "y", "on", "true", "yes")

    /** Support for booleans as data. */
    given Data[Boolean] = _.toLowerCase match
      case _false if falses(_false) => Some(false)
      case _true if trues(_true) => Some(true)
      case _ => None

    /** Support for integers as data. */
    given Data[Int] = data =>
      try Some(JInt.parseInt(data)) catch case _: NumberFormatException => None

    /** Support for longs as data. */
    given Data[Long] = data =>
      try Some(JLong.parseLong(data)) catch case _: NumberFormatException => None

    /** Support for floats as data. */
    given Data[Float] = data =>
      try Some(JFloat.parseFloat(data)) catch case _: NumberFormatException => None

    /** Support for doubles as data. */
    given Data[Double] = data =>
      try Some(JDouble.parseDouble(data)) catch case _: NumberFormatException => None

    /** Support for strings as data. */
    given Data[String] = Some(_)

    /** Support for IDs as data. */
    given Data[ID] = ID decode _

    /** Support for locations as data. */
    given Data[Location] = Location decode _

    /**
     * Returns the given data for the specified type.
     *
     * @tparam T The type to return the given data for.
     * @return The given data for the specified type.
     */
    def apply[T: Data]: Data[T] = summon[Data[T]]
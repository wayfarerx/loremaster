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

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

import zio.{IO, Task, UIO}

import model.*


/**
 * Definition of the configuration service.
 */
trait Configuration:

  import Configuration.Data

  /**
   * Returns the value of the specified configuration entry.
   *
   * @tparam T The type of value to return.
   * @param key The key of the configuration entry to return.
   * @return The value of the specified configuration entry.
   */
  def apply[T: Data](key: String): IO[ConfigurationProblem, T] = get(key) flatMap {
    case Some(data) => UIO(data)
    case None => IO.fail(ConfigurationProblem(Messages.undefinedConfiguration(key, Data[T].`type`)))
  }

  /**
   * Returns the value of the specified configuration entry if it exists.
   *
   * @tparam T The type of value to return if it exists.
   * @param key The key of the configuration entry to return.
   * @return The value of the specified configuration entry if it exists.
   */
  def get[T: Data](key: String): IO[ConfigurationProblem, Option[T]]

  /**
   * Returns the value of the specified configuration entry if it exists or the supplied default.
   *
   * @tparam T The type of value to return if it exists.
   * @param key     The key of the configuration entry to return.
   * @param default The default to return if the configuration entry does not exist.
   * @return The value of the specified configuration entry if it exists or the supplied default.
   */
  def getOrElse[T: Data](key: String, default: => T): IO[ConfigurationProblem, T] =
    get(key).map(_ getOrElse default)

/**
 * Definitions associated with configurations.
 */
object Configuration extends ((String => IO[ConfigurationProblem, Option[String]]) => Configuration) :

  /** The empty configuration. */
  val empty: Configuration = Configuration(_ => UIO.none)

  /**
   * Creates a configuration backed by a source.
   *
   * @param source The source of the configuration.
   * @return A configuration backed by a source.
   */
  override def apply(source: String => IO[ConfigurationProblem, Option[String]]): Configuration =
    new Configuration :
      override def get[T: Data](key: String) = source(key).map(_ flatMap Data[T].apply)

  /**
   * Support for a specific type of configuration data.
   *
   * @tparam T The type of configuration data to support.
   */
  trait Data[T] extends (String => Option[T]) :

    /** The type of this data. */
    def `type`: String

  /**
   * Definitions of the supported configuration data types.
   */
  object Data:

    /** Boolean data values. */
    private[this] val BooleanValues = {
      Iterator("1", "t", "y", "on", "true", "yes").map(_ -> true) ++
        Iterator("0", "f", "n", "off", "false", "no").map(_ -> false)
    }.toMap

    /** Support for booleans as data. */
    given Data[Boolean] = define("Boolean")(BooleanValues get _.toLowerCase)

    /** Support for bytes as data. */
    given Data[Byte] = define("Byte") { data =>
      try Some(java.lang.Byte parseByte data) catch case _: NumberFormatException => None
    }

    /** Support for shorts as data. */
    given Data[Short] = define("Short") { data =>
      try Some(java.lang.Short parseShort data) catch case _: NumberFormatException => None
    }

    /** Support for integers as data. */
    given Data[Int] = define("Int") { data =>
      try Some(java.lang.Integer parseInt data) catch case _: NumberFormatException => None
    }

    /** Support for longs as data. */
    given Data[Long] = define("Long") { data =>
      try Some(java.lang.Long parseLong data) catch case _: NumberFormatException => None
    }

    /** Support for floats as data. */
    given Data[Float] = define("Float") { data =>
      try Some(java.lang.Float parseFloat data) catch case _: NumberFormatException => None
    }

    /** Support for doubles as data. */
    given Data[Double] = define("Double") { data =>
      try Some(java.lang.Double parseDouble data) catch case _: NumberFormatException => None
    }

    /** Support for characters as data. */
    given Data[Char] = define("Char") { data =>
      if data.length == 1 then Some(data charAt 0) else None
    }

    /** Support for strings as data. */
    given Data[String] = define("String")(Some(_))

    /** Support for finite durations as data. */
    given Data[FiniteDuration] = define("FiniteDuration") { data =>
      try Some(Duration(data)) collect { case result: FiniteDuration => result }
      catch case _: NumberFormatException => None
    }

    /** Support for IDs as data. */
    given Data[ID] = define("ID")(ID.decode)

    /** Support for locations as data. */
    given Data[Location] = define("Location")(Location.decode(_))

    /**
     * Returns the given data for the specified type.
     *
     * @tparam T The type to return the given data for.
     * @return The given data for the specified type.
     */
    inline def apply[T: Data]: Data[T] = summon[Data[T]]

    /**
     * Defines a new type of configuration data.
     *
     * @tparam T The type of configuration data to support.
     * @param `type` The name of the data type.
     * @param f      The function that decodes data.
     * @return A new type of configuration data.
     */
    def define[T](`type`: String)(f: String => Option[T]): Data[T] =
      val _type = `type`
      new Data :

        override def `type` = _type

        override def apply(data: String) = f(data)
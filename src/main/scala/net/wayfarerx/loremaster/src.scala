/* src.scala
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

import cats.data.NonEmptyList
import zio.{Task, UIO}

/** The zero effect. */
val zero: UIO[Int] = UIO(0)

/**
 * Creates a pure unit effect.
 *
 * @return A pure unit effect.
 */
def unit: UIO[Unit] = UIO.unit

/**
 * Creates a pure effect from the specified value.
 *
 * @tparam T The type of the pure value.
 * @param value The value to use.
 * @return A pure effect from the specisfied value.
 */
def pure[T](value: => T): UIO[T] = UIO(value)

/**
 * Creates a none effect.
 *
 * @return A none effect.
 */
def none: UIO[Option[Nothing]] = UIO.none

/**
 * Creates a some effect from the specified value.
 *
 * @tparam T The type of the value.
 * @param value The value to use.
 * @return A some effect from the specified value.
 */
def some[T](value: => T): UIO[Option[T]] = UIO some value

/**
 * Creates a nil effect.
 *
 * @tparam T The type of the list element.
 * @return A nil effect.
 */
def nil[T]: UIO[List[T]] = UIO(Nil)

/**
 * Creates a problematic effect from the specified message.
 *
 * @param message The message that describes the problem.
 * @return A problematic effect from the specified message.
 */
def fail(message: String): Task[Nothing] = Task fail Problem(message)

/**
 * Creates a problematic effect with the specified message and cause.
 *
 * @param message The message that describes the problem.
 * @param cause   The cause of the problem.
 * @return A problematic effect with the specified message and cause.
 */
def fail(message: String, cause: Throwable): Task[Nothing] = Task fail Problem(message, cause)

/** Definition of the ID type. */
opaque type ID = String

/**
 * Extensions to the ID type.
 */
extension (self: ID) {

  /** Returns the encoded form of the ID. */
  def encoded: String = self

}

/**
 * Factory for IDs.
 */
object ID extends (String => Task[ID]) :

  import java.net.URLEncoder
  import java.nio.charset.StandardCharsets

  /** The ordering of IDs. */
  given ordering: Ordering[ID] = Ordering[String]

  /** A regex that matches invalid ID content. */
  private val Invalid = """[/\\\s]|\.\.""".r

  /**
   * Attempts to decode an ID from a string.
   *
   * @param string The string to decode the ID from.
   * @return The result of attempting to decode an ID from a string.
   */
  def decode(string: String): Option[ID] = nonEmpty(string) flatMap materialize

  /**
   * Creates an ID from a string.
   *
   * @param string The string to create the ID from.
   * @return An ID created from the specified string.
   */
  override def apply(string: String): Task[ID] =
    nonEmpty(string).fold(fail("Invalid empty ID."))(materialize(_).fold(fail(s"Invalid ID: $string."))(pure))

  /**
   * Returns the specified string if it is non-`null` and non-empty.
   *
   * @param string The string that should not be `null` or empty.
   * @return The specified string if it is non-`null` and non-empty.
   */
  private def nonEmpty(string: String): Option[String] = Option(string) filterNot (_.isEmpty)

  /**
   * Returns an ID if the specified non-empty string is valid.
   *
   * @param notEmpty The non-empty string to materialize into an ID.
   * @return An ID if the specified non-empty string is valid.
   */
  private def materialize(notEmpty: String): Option[ID] =
    Invalid.findFirstIn(notEmpty).fold(Some(notEmpty) filterNot (_ == "."))(_ => None) map { materialized =>
      URLEncoder.encode(materialized, StandardCharsets.UTF_8)
    }

/** Definition of the Location type. */
opaque type Location = NonEmptyList[ID]

/**
 * Extensions to the Location type.
 */
extension (self: Location) {

  /** The encoded form of the Location. */
  def encoded: String = self.iterator mkString "/"

  /** The fist ID in the location. */
  def head: ID = self.head

  /** The non-first IDs in the location. */
  def tail: List[ID] = self.tail

  /** The non-last IDs in the location. */
  def init: List[ID] = self.init

  /** The last ID in the location. */
  def last: ID = self.last

  /** Appends that ID to the Location. */
  def :+(id: ID): Location = self :+ id

  /** Prepends that ID to the Location. */
  def +:(id: ID): Location = id :: self

  /** Appends that Location to the Location. */
  def :++(that: Location): Location = self ::: that

  /** Prepends that Location to the Location. */
  def ++:(that: Location): Location = that ::: self

  /** Reverses the order of the IDs in the location. */
  def reverse: Location = self.reverse

}

/**
 * Factory for Locations.
 */
object Location extends (String => Task[Location]) :

  /** A regex that matches sequences of seperator characters. */
  private val Seperators = """[/\\]+""".r

  /**
   * Returns a location composed of the specified IDs.
   *
   * @param head The head of the ID sequence.
   * @param tail The tail of the ID sequence.
   * @return A location composed of the specified IDs.
   */
  def of(head: ID, tail: ID*): Location = NonEmptyList.of(head, tail *)

  /**
   * Returns a location composed of the specified IDs.
   *
   * @param list The list of IDs to compose a location of.
   * @return A location composed of the specified IDs.
   */
  def from(list: List[ID]): Option[Location] =
    Option(list) filterNot (_.isEmpty) map (l => of(l.head, l.tail *))

  /**
   * Attempts to decode a location from a string.
   *
   * @param string The string to decode the location from.
   * @return The result of attempting to decode a location from a string.
   */
  def decode(string: String): Option[Location] = nonEmpty(string) flatMap materialize

  /**
   * Attempts to decode a location from a sequence of strings.
   *
   * @param strings The sequence of strings to decode the location from.
   * @return The result of attempting to decode a location from a sequence of strings.
   */
  def decode(strings: Seq[String]): Option[Location] = nonEmpty(strings) flatMap materialize

  /**
   * Creates a location from a string.
   *
   * @param string The string to create the location from.
   * @return A location created from the specified string.
   */
  override def apply(string: String): Task[Location] =
    nonEmpty(string).fold(fail("Invalid empty Location.")) { notEmpty =>
      materialize(notEmpty).fold(fail(s"Invalid Location: ${notEmpty mkString "/"}."))(pure)
    }

  /**
   * Creates a location from a sequence of strings.
   *
   * @param strings The sequence of strings to create the location from.
   * @return A location created from the specified sequence of strings.
   */
  def apply(strings: Seq[String]): Task[Location] =
    nonEmpty(strings).fold(fail("Invalid empty Location.")) { notEmpty =>
      materialize(notEmpty).fold(fail(s"Invalid Location: ${notEmpty mkString "/"}."))(pure)
    }

  /**
   * Returns the specified string split into a sequence of non-empty strings.
   *
   * @param string The string that, if non-`null`, is split, filtered and returned.
   * @return The specified string split into a sequence of non-empty strings.
   */
  private def nonEmpty(string: String): Option[Seq[String]] =
    Option(string) map (Seperators.split(_).iterator filterNot (_.isEmpty)) filterNot (_.isEmpty) map (_.toSeq)

  /**
   * Returns the specified strings split into a sequence of non-empty stringsa.
   *
   * @param strings The sequence of strings that, if non-`null`, are split, filtered, flattened and returned.
   * @return The specified strings split into a sequence of non-empty strings.
   */
  private def nonEmpty(strings: Seq[String]): Option[Seq[String]] =
    Option(strings) map (_.iterator.flatMap(nonEmpty).flatten) filterNot (_.isEmpty) map (_.toSeq)

  /**
   * Returns a Location from a sequence of non-empty strings if they are all valid.
   *
   * @param ids The sequence of non-empty strings to materialize a Location from.
   * @return A Location from a sequence of non-empty strings if they are all valid.
   */
  private def materialize(ids: Seq[String]): Option[Location] = for
    list <- ids.foldRight(Option(List.empty[ID]))((head, tail) => tail flatMap (t => ID decode head map (_ :: t)))
    result <- NonEmptyList.fromList(list)
  yield result

/**
 * Extensions to the StringContext type.
 */
extension (context: StringContext) {

  /**
   * Enables the `id` string constant prefix.
   *
   * @param args The arguments passed to the string context.
   * @return An ID derived from the string context and aguments.
   */
  def id(args: Any*): ID = ID.decode(context.s(args *)).get

  /**
   * Enables the `location` string constant prefix.
   *
   * @param args The arguments passed to the string context.
   * @return A Location derived from the string context and aguments.
   */
  def location(args: Any*): Location = Location.decode(context.s(args *)).get

}
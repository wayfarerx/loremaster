/* Location.scala
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

import scala.math.Ordering.Implicits.given

import cats.Foldable
import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder}

/**
 * Definition of the location type.
 *
 * @param path The non-empty list of IDs that define this location.
 */
case class Location(path: NonEmptyList[ID]):

  /** The number of IDs in this location. */
  def size: Int = path.size

  /** The fist ID in this location. */
  def head: ID = path.head

  /** The non-first IDs in this location. */
  def tail: List[ID] = path.tail

  /** The non-last IDs in this location. */
  def init: List[ID] = path.init

  /** The last ID in this location. */
  def last: ID = path.last

  /** This location with the IDs in reverse order. */
  def reverse: Location = Location(path.reverse)

  /** Appends that ID to this location */
  def :+(id: ID): Location = Location(path :+ id)

  /** Prepends that ID to this location */
  def +:(id: ID): Location = Location(id :: path)

  /** Appends that location to this location */
  def :++(that: Location): Location = Location(path ::: that.path)

  /** Prepends that location to this location */
  def ++:(that: Location): Location = Location(that.path ::: path)

  /**
   * Appends the specified suffix to the last ID in this location.
   *
   * @param suffix The suffix to append to the last ID in this location.
   * @return This location with the specified suffix appended to the last ID.
   */
  def withSuffix(suffix: String): Option[Location] = for
    suffixed <- ID.decode(path.last.value + suffix)
    result <- Location.from(path.init :+ suffixed)
  yield result

  /* Return a string representation of this location. */
  override def toString: String = path.iterator mkString Location.Separator

/**
 * Factory for locations.
 */
object Location extends (NonEmptyList[ID] => Location) :

  /** The ordering of locations. */
  given Ordering[Location] = Ordering by (_.path.toList)

  /** The encoding of locations to JSON. */
  given Encoder[Location] = Encoder[String].contramap(_.toString)

  /** The decoding of locations from JSON. */
  given Decoder[Location] = Decoder[String].emap(path => decode(path) toRight Messages.invalidLocation(path))

  /** The canonical separator character. */
  private val Separator = "/"

  /** A regex that matches sequences of separator characters. */
  private[this] val Separators = """[/\\]+""".r

  /**
   * Creates a location with a path of the specified IDs.
   *
   * @param head The head ID of the path.
   * @param tail The tail IDs of the path.
   * @return A location with a path of the specified IDs.
   */
  def of(head: ID, tail: ID*): Location =
    apply(NonEmptyList.of(head, tail *))

  /**
   * Creates a location with a path of the specified IDs.
   *
   * @tparam F The type of foldable ID collection.
   * @param path The IDs of the path.
   * @return A location with a path of the specified IDs.
   */
  def from[F[_] : Foldable](path: F[ID]): Option[Location] =
    NonEmptyList.fromFoldable(path) map apply


  /**
   * Decodes a location from zero or more strings.
   *
   * @param strings The strings to decode a location from.
   * @return A location decoded from zero or more strings.
   */
  def decode(strings: String*): Option[Location] =
    decoding(Vector.empty, strings.iterator.flatMap(Separators.split).filterNot(_.isEmpty).toList) map apply

  /**
   * Implementation of the string decoder.
   *
   * @param stack     The stack of IDs to decode into.
   * @param remaining The remaining strings to decode.
   * @return The decoded non-empty list of IDs.
   */
  @annotation.tailrec
  private[this] def decoding(stack: Vector[ID], remaining: List[String]): Option[NonEmptyList[ID]] = remaining match
    case "." :: tail =>
      decoding(stack, tail)
    case ".." :: tail =>
      if stack.isEmpty then None
      else decoding(stack.init, tail)
    case head :: tail =>
      ID.decode(head) match
        case Some(id) => decoding(stack :+ id, tail)
        case None => None
    case Nil => NonEmptyList.fromFoldable(stack)
/* Location.scala
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

import math.Ordering.Implicits.given

import cats.data.NonEmptyList

import zio.Task

/**
 * Definition of the location type.
 *
 * @param value The non-empty list of IDs that define this location.
 */
case class Location(value: NonEmptyList[ID]):

  /** The number of IDs in this location. */
  def size: Int = value.size

  /** The fist ID in this location. */
  def head: ID = value.head

  /** The non-first IDs in this location. */
  def tail: List[ID] = value.tail

  /** The non-last IDs in this location. */
  def init: List[ID] = value.init

  /** The last ID in this location. */
  def last: ID = value.last

  /** This location with the IDs in reverse order. */
  def reverse: Location = Location(value.reverse)

  /** Appends that ID to this location */
  def :+(id: ID): Location = Location(value :+ id)

  /** Prepends that ID to this location */
  def +:(id: ID): Location = Location(id :: value)

  /** Appends that location to this location */
  def :++(that: Location): Location = Location(value ::: that.value)

  /** Prepends that location to this location */
  def ++:(that: Location): Location = Location(that.value ::: value)

  /**
   * Appends the specified suffix to the last ID in this location.
   *
   * @param suffix The suffix to append to the last ID in this location.
   * @return This location with the specified suffix appended to the last ID.
   */
  def appendToLast(suffix: String): Option[Location] = for
    last <- ID fromString value.last.value + suffix
    result <- NonEmptyList fromList value.init :+ last map (Location(_))
  yield result

  /**
   * Appends the specified suffix to the last ID in this location.
   *
   * @param suffix The suffix to append to the last ID in this location.
   * @return This location with the specified suffix appended to the last ID.
   */
  def appendedToLast(suffix: String): Task[Location] =
    appendToLast(suffix).fold(fail(s"Cannot append suffix $suffix to location $toString."))(pure(_))

  /* Return a string representation of this location. */
  override def toString: String = value.iterator mkString Location.Separator

/**
 * Factory for locations.
 */
object Location extends (NonEmptyList[ID] => Location) :

  /** The ordering of locations. */
  given Ordering[Location] = Ordering.by(_.value.toList)

  /** The canonical separator character. */
  private val Separator = "/"

  /** A regex that matches sequences of separator characters. */
  private val Separators = """[/\\]+""".r

  /**
   * Returns a location composed of the specified IDs.
   *
   * @param head The head of the ID sequence.
   * @param tail The tail of the ID sequence.
   * @return A location composed of the specified IDs.
   */
  def of(head: ID, tail: ID*): Location =
    Location(NonEmptyList.of(head, tail *))

  /**
   * Returns a location composed of the specified IDs.
   *
   * @param ids The ID sequence.
   * @return A location composed of the specified IDs.
   */
  def from(ids: ID*): Option[Location] =
    ids.headOption map (of(_, ids.tail *))


  /**
   * Decodes a location from zero or more values.
   *
   * @param values The values to decode a location from.
   * @return A location decoded from zero or more values.
   */
  def decode(values: String*): Option[Location] =
    NonEmptyList fromFoldable values.iterator.flatMap(Separators.split).flatMap(ID.fromString).toSeq map Location.apply

  /**
   * Creates a location from zero or more values.
   *
   * @param values The values to create a location from.
   * @return A location created from zero or more values.
   */
  def create(values: String*): Task[Location] =
    decode(values *).fold(fail(s"Invalid location: ${values mkString Separator}."))(pure(_))
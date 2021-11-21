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

package net.wayfarerx.loremaster.model

import scala.math.Ordering.Implicits.given

import cats.data.NonEmptyList

/**
 * Definition of the location type.
 *
 * @param elements The non-empty list of IDs that define this location.
 */
case class Location(elements: NonEmptyList[ID]):

  /** The number of IDs in this location. */
  def size: Int = elements.size

  /** The fist ID in this location. */
  def head: ID = elements.head

  /** The non-first IDs in this location. */
  def tail: List[ID] = elements.tail

  /** The non-last IDs in this location. */
  def init: List[ID] = elements.init

  /** The last ID in this location. */
  def last: ID = elements.last

  /** This location with the IDs in reverse order. */
  def reverse: Location = Location(elements.reverse)

  /** Appends that ID to this location */
  def :+(id: ID): Location = Location(elements :+ id)

  /** Prepends that ID to this location */
  def +:(id: ID): Location = Location(id :: elements)

  /** Appends that location to this location */
  def :++(that: Location): Location = Location(elements ::: that.elements)

  /** Prepends that location to this location */
  def ++:(that: Location): Location = Location(that.elements ::: elements)

  /**
   * Appends the specified suffix to the last ID in this location.
   *
   * @param suffix The suffix to append to the last ID in this location.
   * @return This location with the specified suffix appended to the last ID.
   */
  def appendToLast(suffix: String): Option[Location] = for
    last <- ID.decode(elements.last.value + suffix)
    result <- Location.from(elements.init :+ last)
  yield result

  /* Return a string representation of this location. */
  override def toString: String = elements.iterator mkString Location.Separator

/**
 * Factory for locations.
 */
object Location extends (NonEmptyList[ID] => Location) :

  /** The ordering of locations. */
  given Ordering[Location] = Ordering.by(_.elements.toList)

  /** The canonical separator character. */
  private val Separator = "/"

  /** A regex that matches sequences of separator characters. */
  private[this] val Separators = """[/\\]+""".r

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
  def from(ids: Iterable[ID]): Option[Location] =
    ids.headOption map (of(_, ids.tail.toSeq *))


  /**
   * Decodes a location from zero or more values.
   *
   * @param values The values to decode a location from.
   * @return A location decoded from zero or more values.
   */
  def decode(values: String*): Option[Location] =
    from(values.iterator flatMap Separators.split flatMap ID.decode to Iterable)
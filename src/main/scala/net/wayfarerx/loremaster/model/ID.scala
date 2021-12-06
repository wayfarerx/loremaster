/* ID.scala
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

import io.circe.{Decoder, Encoder}

/**
 * Definition of the ID type.
 *
 * @param value The valid string representation of this ID.
 */
case class ID private(value: String):

  /* Return a string representation of this ID. */
  override def toString: String = value

/**
 * Factory for IDs.
 */
object ID:

  /** The ordering of IDs. */
  given Ordering[ID] = Ordering by (_.value)

  /** The encoding of IDs to JSON. */
  given Encoder[ID] = Encoder[String] contramap (_.value)

  /** The decoding of IDs from JSON. */
  given Decoder[ID] = Decoder[String] emap (value => decode(value) toRight Messages.invalidId(value))

  /** The dots that are not allowed to define IDs. */
  private[this] val Dots = Set('.')

  /** The slashes that are not allowed to appear in IDs. */
  private[this] val Slashes = Set('/', '\\')

  /**
   * Decodes an ID from a string.
   *
   * @param string The string to decode an ID from.
   * @return An ID decoded from the specified string.
   */
  def decode(string: String): Option[ID] =
    if string.isEmpty || string.forall(Dots) || string.exists(Slashes) then None
    else Some(ID(string))
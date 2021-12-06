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
package model

import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder, parser}

/** The given non empty list encoder. */
given[T: Encoder]: Encoder[NonEmptyList[T]] = Encoder[List[T]] contramap (_.toList)

/** The given non empty list decoder. */
given[T: Decoder]: Decoder[NonEmptyList[T]] = Decoder[List[T]] emap { list =>
  NonEmptyList fromList list toRight Messages.invalidNonEmptyList(list.mkString("[", ", ", "]"))
}

/**
 * Encodes data into a JSON string.
 *
 * @tparam T The type of data to encode.
 * @param data The data to encode.
 * @return The data encoded as a JSON sting.
 */
def encodeJson[T: Encoder](data: T): String = Encoder[T].apply(data).spaces2

/**
 * Attempts to decode data from a JSON string.
 *
 * @tparam T The type of data to decode.
 * @param json The JSON to decode.
 * @return The result of attempting to decode data from a JSON string.
 */
def decodeJson[T: Decoder](json: String): Either[Exception, T] = parser.decode[T](json)

/**
 * Extensions to the StringContext type.
 */
extension (context: StringContext) {

  /**
   * Enables the `id` string constant prefix.
   *
   * @param args The arguments passed to the string context.
   * @return An ID derived from the string context and arguments.
   */
  def id(args: Any*): ID = ID.decode(context.s(args *)).get

  /**
   * Enables the `location` string constant prefix.
   *
   * @param args The arguments passed to the string context.
   * @return A Location derived from the string context and arguments.
   *
   */
  def location(args: Any*): Location = Location.decode(context.s(args *)).get

}
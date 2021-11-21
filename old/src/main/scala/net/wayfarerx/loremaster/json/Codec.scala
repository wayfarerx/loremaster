/* Codec.scala
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
package json

import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, Json}

/**
 * Definition of a codec for the specified type of data.
 *
 * @tparam T The type of data this codec supports.
 */
trait Codec[T]:

  /** The JSON encoder this codec provides. */
  final lazy val encoder: Encoder[T] = encode(_)

  /** The JSON decoder this codec provides. */
  final lazy val decoder: Decoder[T] = decode(_)

  /** The type of this codec. */
  def `type`: String

  /**
   * Encodes the specified data into JSON.
   *
   * @param data The data to encode.
   * @return The specified data encoded into JSON.
   */
  def encode(data: T): Json

  /**
   * Decodes data from the specified JSON.
   *
   * @param cursor The cursor into the JSON do decode.
   * @return The data decoded from the specified JSON.
   */
  def decode(cursor: ACursor): Decoder.Result[T]

  /**
   * Returns a failure outcome when decoding data.
   *
   * @param cursor The cursor that points to the invalid data.
   * @return A failure outcome when decoding data.
   */
  protected final def decodeFailed(cursor: ACursor): Decoder.Result[Nothing] =
    Left(DecodingFailure(s"Failed to decode ${`type`}.", cursor.history))

/**
 * Factory for codecs.
 */
object Codec:

  /**
   * Returns the given codec for the specified type.
   *
   * @tparam T The type to return the given codec for.
   * @param codec The given codec to return.
   * @return The given codec for the specified type.
   */
  def apply[T](using codec: Codec[T]): Codec[T] = codec
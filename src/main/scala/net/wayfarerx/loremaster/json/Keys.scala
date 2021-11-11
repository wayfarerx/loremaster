/* Keys.scala
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

import io.circe.{ACursor, Decoder, Json}

import model.*

/**
 * Implementation of the key JSON codec.
 */
case object Keys extends Codec[Key]:

  /* The type of this codec. */
  override def `type`: String = "Key"

  /* Encode the specified data into JSON. */
  override def encode(key: Key): Json = key match
    case Key.Start => Json.arr()
    case Key.From(token) => Json.arr(Tokens.encode(token))

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[Key] =
    cursor.focus.flatMap(_.asArray).fold(decodeFailed(cursor)) { array =>
      if array.isEmpty then Right(Key.Start)
      else if array.length == 1 then Tokens.decode(cursor.downArray) map Key.From.apply
      else decodeFailed(cursor)
    }
/* Targets.scala
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
 * Implementation of the target JSON codec.
 */
case object Targets extends Codec[Target]:

  /* The type of this codec. */
  override def `type`: String = "Target"

  /* Encode the specified data into JSON. */
  override def encode(target: Target): Json = target match
    case Target.Continue(token) => Json.arr(Tokens.encode(token))
    case Target.End => Json.arr()

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[Target] =
    cursor.focus.flatMap(_.asArray).fold(decodeFailed(cursor)) { array =>
      if array.isEmpty then Right(Target.End)
      else if array.length == 1 then Tokens.decode(cursor.downArray) map Target.Continue.apply
      else decodeFailed(cursor)
    }
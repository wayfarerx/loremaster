/* Strings.scala
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

/**
 * Implementation of the string JSON codec.
 */
private case object Strings extends Codec[String]:

  /* The type of this codec. */
  override val `type`: String = "String"

  /* Encode the specified data into JSON. */
  override def encode(string: String): Json =
    Json fromString string

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[String] =
    cursor.focus.flatMap(_.asString).fold(decodeFailed(cursor))(Right(_))

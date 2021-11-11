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
package json

import cats.data.{NonEmptyList, NonEmptyMap}

import io.circe.{Decoder, Encoder}
import io.circe.parser

import zio.{Task, UIO}

import model.*

/** The given long codec. */
given Codec[Long] = Longs

/** The given string codec. */
given Codec[String] = Strings

/** The given ID codec. */
given Codec[ID] = IDs

/** The given location codec. */
given Codec[Location] = Locations

/** The given token codec. */
given Codec[Token] = Tokens

/** The given key codec. */
given Codec[Key] = Keys

/** The given target codec. */
given Codec[Target] = Targets

/** The given non-empty list codec. */
given[T: Codec]: Codec[NonEmptyList[T]] = NonEmptyLists[T]()

/** The given non-empty map codec. */
given[K: Ordering : Codec, V: Codec]: Codec[NonEmptyMap[K, V]] = NonEmptyMaps[K, V]()

/** The given encoder codec support. */
given[T: Codec]: Encoder[T] = Codec[T].encoder

/** The given decoder codec support. */
given[T: Codec]: Decoder[T] = Codec[T].decoder

/**
 * Emits a value as JSON.
 *
 * @tparam T The type of value to emit.
 * @param value The value to emit.
 * @return The value emitted as JSON.
 */
def emit[T: Encoder](value: T): UIO[String] =
  pure(Encoder[T].apply(value).spaces2)

/**
 * Parses JSON data into a value.
 *
 * @tparam T The type of value to parse.
 * @param data The JSON data to parse.
 * @return The JSON data parsed into a value.
 */
def parse[T: Decoder](data: String): Task[T] =
  parser.decode[T](data).fold(fail("Failed to parse JSON.", _), pure(_))
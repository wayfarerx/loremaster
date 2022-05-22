/* src.scala
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

import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder, parser, Error => JsonError}

/** The name of the application. */
val Application: String = "Loremaster"

/** The given non empty list encoder. */
given[T: Encoder]: Encoder[NonEmptyList[T]] = Encoder[List[T]].contramap(_.toList)

/** The given non empty list decoder. */
given[T: Decoder]: Decoder[NonEmptyList[T]] = Decoder[List[T]] emap {
  NonEmptyList fromList _ toRight s"Unable to decode non-empty list from empty list."
}

/**
 * Emits a JSON value as a string.
 *
 * @tparam T The type of value to emit.
 * @param value The value to emit.
 * @return The JSON value emitted as a sting.
 */
def emit[T: Encoder](value: T): String = Encoder[T].apply(value).spaces2

/**
 * Attempts to parse a JSON value from a string.
 *
 * @tparam T The type of value to parse.
 * @param json The string to parse.
 * @return The result of attempting to parse a JSON-compatible value from a string.
 */
def parse[T: Decoder](json: String): Either[JsonError, T] = parser.decode[T](json)

/**
 * Describes a throwable.
 *
 * @param thrown The throwable to describe.
 * @return The description of the throwable.
 */
def describe(thrown: Throwable): String =
  s"${thrown.getClass.getSimpleName}${Option(thrown.getMessage).filterNot(_.isEmpty).fold("")(msg => s"($msg)")}"
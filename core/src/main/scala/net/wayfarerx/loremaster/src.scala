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

import io.circe.{Decoder, Encoder, parser}

/** The given non empty list encoder. */
given[T: Encoder]: Encoder[NonEmptyList[T]] = Encoder[List[T]].contramap(_.toList)

/** The given non empty list decoder. */
given[T: Decoder]: Decoder[NonEmptyList[T]] = Decoder[List[T]] emap { list =>
  NonEmptyList.fromList(list) toRight s"Unable to decode non-empty list from ${list.mkString("[", ", ", "]")}"
}

/** The name of the Loremaster application. */
val Loremaster: String = "Loremaster"

/**
 * Describes a throwable.
 *
 * @param thrown The throwable to describe.
 * @return The description of the throwable.
 */
def describe(thrown: Throwable): String =
  s"${thrown.getClass.getSimpleName}${Option(thrown.getMessage).filterNot(_.isEmpty).fold("")(msg => s"($msg)") }"

/**
 * Emits a JSON value as a string.
 *
 * @tparam T The type of value to emit.
 * @param value The value to emit.
 * @return The JSON value emitted as a sting.
 */
def emitJson[T: Encoder](value: T): String = Encoder[T].apply(value).spaces2

/**
 * Attempts to parse a JSON value from a string.
 *
 * @tparam T The type of value to parse.
 * @param json The string to parse.
 * @return The result of attempting to parse a JSON value from a string.
 */
def parseJson[T: Decoder](json: String): Either[Throwable, T] = parser.decode[T](json)
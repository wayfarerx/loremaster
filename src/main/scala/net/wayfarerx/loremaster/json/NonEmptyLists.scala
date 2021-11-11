/* NonEmptyLists.scala
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

import cats.data.NonEmptyList

import io.circe.{ACursor, Decoder, Json}

/**
 * Implementation of the non-empty list JSON codec.
 */
private case class NonEmptyLists[T: Codec]() extends Codec[NonEmptyList[T]] :

  /* The type of this codec. */
  override def `type`: String = s"[${Codec[T].`type`}]"

  /* Encode the specified data into JSON. */
  override def encode(nonEmptyList: NonEmptyList[T]): Json =
    Json.arr(nonEmptyList.iterator.map(Codec[T].encode).toSeq *)

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[NonEmptyList[T]] =

    @annotation.tailrec
    def continue(results: Vector[T], index: Int, size: Int): Decoder.Result[Vector[T]] =
      if index >= size then Right(results) else
        val element = cursor.downN(index)
        if element.succeeded then
          Codec[T].decode(element) match
            case Left(failure) => Left(failure)
            case Right(result) => continue(results :+ result, index + 1, size)
        else decodeFailed(element)

    cursor.focus.flatMap(_.asArray).fold(decodeFailed(cursor)) { array =>
      continue(Vector.empty, 0, array.size) flatMap { elements =>
        elements.headOption.fold(decodeFailed(cursor))(Right apply NonEmptyList.of(_, elements.tail *))
      }
    }
/* NonEmptyMaps.scala
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

import cats.data.NonEmptyMap
import cats.kernel.Order

import io.circe.{ACursor, Decoder, Json}

/**
 * Implementation of the non-empty map JSON codec.
 */
private case class NonEmptyMaps[K: Ordering : Codec, V: Codec]() extends Codec[NonEmptyMap[K, V]] :

  /* The type of this codec. */
  override def `type`: String = s"{${Codec[K].`type`} -> ${Codec[V].`type`}}"

  /* Encode the specified data into JSON. */
  override def encode(data: NonEmptyMap[K, V]): Json =
    Json.arr(data.toSortedMap.iterator.map { case (k, v) => Json.arr(Codec[K].encode(k), Codec[V].encode(v)) }.toSeq *)

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[NonEmptyMap[K, V]] =

    @annotation.tailrec
    def continue(results: Vector[(K, V)], index: Int, size: Int): Decoder.Result[Vector[(K, V)]] =
      if index >= size then Right(results) else
        val entry = cursor.downN(index)
        if entry.focus flatMap (_.asArray) exists (_.size == 2) then
          Codec[K].decode(entry downN 0) match
            case Left(failure) => Left(failure)
            case Right(key) =>
              Codec[V].decode(entry downN 1) match
                case Left(failure) => Left(failure)
                case Right(value) => continue(results :+ (key -> value), index + 1, size)
        else decodeFailed(entry)

    cursor.focus.flatMap(_.asArray).fold(decodeFailed(cursor)) { array =>
      continue(Vector.empty, 0, array.size) flatMap { entries =>
        entries.headOption
          .fold(decodeFailed(cursor))(Right apply NonEmptyMap.of(_, entries.tail *)(Order.fromOrdering[K]))
      }
    }
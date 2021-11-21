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

import scala.collection.immutable.SortedMap

import cats.data.{NonEmptyList, NonEmptyMap}
import cats.kernel.Order

/** Books are a non-empty list of paragraph strings. */
type Book = NonEmptyList[String]

/** Factory for books. */
object Book extends NonEmptyListAlias[String]

/** Sentences are a non-empty list of tokens. */
type Sentence = NonEmptyList[Token]

/** Factory for sentences. */
object Sentence extends NonEmptyListAlias[Token]

/** Paragraphs are a non-empty list of sentences. */
type Paragraph = NonEmptyList[Sentence]

/** Factory for paragraphs. */
object Paragraph extends NonEmptyListAlias[Sentence]

/** Lore is a non-empty list of paragraphs. */
type Lore = NonEmptyList[Paragraph]

/** Factory for lore. */
object Lore extends NonEmptyListAlias[Paragraph]

/** Tables are a non-empty map of tokens to their frequency. */
type Table = NonEmptyMap[Target, Long]

/** Factory for tables. */
object Table extends NonEmptyMapAlias[Target, Long]

/**
 * Base type for companion objects to non-empty list aliases.
 *
 * @tparam T The type of data contained in the non-empty list.
 */
trait NonEmptyListAlias[T]:

  /**
   * Returns a non-empty list composed of the specified elements.
   *
   * @param head The head of the non-empty list.
   * @param tail The tail of the non-empty list.
   * @return A non-empty list composed of the specified elements.
   */
  final def of(head: T, tail: T*): NonEmptyList[T] =
    NonEmptyList(head, tail.toList)

  /**
   * Returns a non-empty list composed of the specified elements.
   *
   * @param elements The element sequence.
   * @return A non-empty list composed of the specified elements.
   */
  final def from(elements: T*): Option[NonEmptyList[T]] =
    elements.headOption map (of(_, elements.tail *))

/**
 * Base type for companion objects to non-empty map aliases.
 *
 * @tparam K The type of key contained in the non-empty map.
 * @tparam V The type of value contained in the non-empty map.
 */
trait NonEmptyMapAlias[K: Ordering, V]:

  /**
   * Returns a non-empty map composed of the specified entries.
   *
   * @param head The head of the non-empty map.
   * @param tail The tail of the non-empty map.
   * @return A non-empty map composed of the specified entries.
   */
  final def of(head: (K, V), tail: (K, V)*): NonEmptyMap[K, V] =
    NonEmptyMap(head, SortedMap(tail *))(Order.fromOrdering[K])

  /**
   * Returns a non-empty map composed of the specified entries.
   *
   * @param entries The entry sequence.
   * @return A non-empty map composed of the specified entries.
   */
  final def from(entries: (K, V)*): Option[NonEmptyMap[K, V]] =
    entries.headOption map (of(_, entries.tail *))

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
  def id(args: Any*): ID = ID.fromString(context.s(args *)).get

  /**
   * Enables the `location` string constant prefix.
   *
   * @param args The arguments passed to the string context.
   * @return A Location derived from the string context and arguments.
   *
   */
  def location(args: Any*): Location = Location.decode(context.s(args *)).get

}
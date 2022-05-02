/* Lore.scala
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
package model

import cats.Foldable
import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * A non-empty list of paragraphs.
 *
 * @param paragraphs The non-empty list of paragraphs.
 */
case class Lore(paragraphs: NonEmptyList[Paragraph])

/**
 * Factory for lore.
 */
object Lore extends (NonEmptyList[Paragraph] => Lore) :

  /** The given encoding of lore to JSON. */
  given Encoder[Lore] = deriveEncoder

  /** The given decoding of lore to JSON. */
  given Decoder[Lore] = deriveDecoder

  /**
   * Creates a lore with the specified paragraphs.
   *
   * @param head The head sentence.
   * @param tail The tail sentences.
   * @return A lore with the specified paragraphs.
   */
  def of(head: Paragraph, tail: Paragraph*): Lore =
    apply(NonEmptyList.of(head, tail *))

  /**
   * Creates a lore with the specified paragraphs.
   *
   * @tparam F The type of foldable paragraph collection.
   * @param paragraphs The paragraphs of the lore.
   * @return A lore with the specified paragraphs.
   */
  def from[F[_] : Foldable](paragraphs: F[Paragraph]): Option[Lore] =
    NonEmptyList.fromFoldable(paragraphs) map apply
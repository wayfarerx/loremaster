/* Paragraph.scala
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

import cats.Foldable
import cats.data.NonEmptyList

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * A non-empty list of sentences.
 *
 * @param sentences The non-empty list of sentences.
 */
case class Paragraph(sentences: NonEmptyList[Sentence])

/**
 * Factory for paragraphs.
 */
object Paragraph extends (NonEmptyList[Sentence] => Paragraph):

  /** The given encoding of paragraphs to JSON. */
  given Encoder[Paragraph] = deriveEncoder

  /** The given decoding of paragraphs to JSON. */
  given Decoder[Paragraph] = deriveDecoder

  /**
   * Creates a paragraph with the specified sentences.
   *
   * @param head The head sentence.
   * @param tail The tail sentences.
   * @return A paragraph with the specified sentences.
   */
  def of(head: Sentence, tail: Sentence*): Paragraph =
    apply(NonEmptyList.of(head, tail*))

  /**
   * Creates a paragraph with the specified sentences.
   *
   * @tparam F The type of foldable sentence collection.
   * @param sentences The sentences of the paragraph.
   * @return A paragraph with the specified sentences.
   */
  def from[F[_] : Foldable](sentences: F[Sentence]): Option[Paragraph] =
    NonEmptyList fromFoldable sentences map apply
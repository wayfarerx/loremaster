/* Sentence.scala
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
 * A non-empty list of tokens.
 *
 * @param tokens The non-empty list of tokens.
 */
case class Sentence(tokens: NonEmptyList[Token])

/**
 * Factory for sentences.
 */
object Sentence extends (NonEmptyList[Token] => Sentence) :

  /** The given encoding of sentences to JSON. */
  given Encoder[Sentence] = deriveEncoder

  /** The given decoding of sentences to JSON. */
  given Decoder[Sentence] = deriveDecoder

  /**
   * Creates a sentence with the specified tokens.
   *
   * @param head The head token.
   * @param tail The tail tokens.
   * @return A sentence with the specified tokens.
   */
  def of(head: Token, tail: Token*): Sentence =
    apply(NonEmptyList.of(head, tail *))

  /**
   * Creates a sentence with the specified tokens.
   *
   * @tparam F The type of foldable token collection.
   * @param tokens The tokens of the sentence.
   * @return A sentence with the specified tokens.
   */
  def from[F[_] : Foldable](tokens: F[Token]): Option[Sentence] =
    NonEmptyList.fromFoldable(tokens) map apply
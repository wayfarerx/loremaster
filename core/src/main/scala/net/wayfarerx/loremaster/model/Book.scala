/* Book.scala
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
 * A non-empty list of paragraph strings.
 *
 * @param paragraphs The non-empty list of paragraph strings.
 */
case class Book(paragraphs: NonEmptyList[String]):

  /* Return a string representation of this book. */
  override def toString: String = paragraphs.iterator mkString "\r\n" * 2

/**
 * Factory for books.
 */
object Book extends (NonEmptyList[String] => Book):

  /** The encoding of books to JSON. */
  given Encoder[Book] = deriveEncoder

  /** The decoding of books from JSON. */
  given Decoder[Book] = deriveDecoder

  /**
   * Creates a book with the specified paragraphs.
   *
   * @param head The head paragraph.
   * @param tail The tail paragraphs.
   * @return A book with the specified paragraphs.
   */
  def of(head: String, tail: String*): Book =
    apply(NonEmptyList.of(head, tail *))

  /**
   * Creates a book with the specified paragraphs.
   *
   * @tparam F The type of foldable string collection.
   * @param paragraphs The paragraphs of the book.
   * @return A book with the specified paragraphs.
   */
  def from[F[_] : Foldable](paragraphs: F[String]): Option[Book] =
    NonEmptyList fromFoldable paragraphs map apply
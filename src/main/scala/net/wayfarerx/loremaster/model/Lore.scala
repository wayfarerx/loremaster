/* Lore.scala
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

import cats.data.NonEmptyList

/**
 * A single piece of lore.
 *
 * @param title   The optional title of this lore.
 * @param author  The optional author of this lore.
 * @param content The paragraphs that comprise the content of this lore.
 */
case class Lore(title: Option[String], author: Option[String], content: NonEmptyList[Paragraph])

/**
 * Factory for lore books.
 */
object Lore extends ((Option[String], Option[String], NonEmptyList[Paragraph]) => Lore) :

  /** The lore book JSON encoder. */
  given io.circe.Encoder[Lore] = io.circe.generic.semiauto.deriveEncoder

  /** The lore book JSON decoder. */
  given io.circe.Decoder[Lore] = io.circe.generic.semiauto.deriveDecoder
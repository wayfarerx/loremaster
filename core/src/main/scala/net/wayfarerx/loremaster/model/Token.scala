/* Token.scala
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

import cats.syntax.functor.*

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * Base type for lore tokens.
 */
sealed trait Token

/**
 * Definitions of the supported lore tokens.
 */
object Token:

  /** The given token ordering. */
  given Ordering[Token] = (x, y) => x match
    case xx@Text(text, _) => y match
      case yy: Text => Ordering[Text].compare(xx, yy)
      case Name(name, _) => compareOrElse(text, name, -1)
    case xx@Name(name, _) => y match
      case Text(text, _) => compareOrElse(name, text, 1)
      case yy: Name => Ordering[Name].compare(xx, yy)

  /** The given encoding of tokens to JSON. */
  given Encoder[Token] = Encoder instance {
    case text: Text => Encoder[Text].apply(text)
    case name: Name => Encoder[Name].apply(name)
  }

  /** The given decoding of tokens from JSON. */
  given Decoder[Token] = List[Decoder[Token]](
    Decoder[Text].widen,
    Decoder[Name].widen,
    Decoder.failedWithMessage(Messages.invalidToken)
  ).reduceLeft((l, r) => l.or(r))

  /**
   * Compares two strings, returning their differnce or the default if they are the same.
   *
   * @param left    The left string to compare.
   * @param right   The right string to compare.
   * @param default The default to return if the left and right are the same.
   * @return The difference of the specified strings or the default if they are the same.
   */
  inline private[this] def compareOrElse(left: String, right: String, default: => Int): Int =
    val result = left compareTo right
    if result == 0 then default else result

  /**
   * A text token.
   *
   * @param text The content of this text token.
   * @param pos  The part of speech of this text token.
   */
  case class Text(text: String, pos: String) extends Token

  /**
   * Factory for text tokens.
   */
  object Text extends ((String, String) => Text) :

    /** The given text token ordering. */
    given Ordering[Text] = (x, y) => compareOrElse(x.text, y.text, x.pos compareTo y.pos)

    /** The given encoding of text tokens to JSON. */
    given Encoder[Text] = deriveEncoder

    /** The given decoding of text tokens from JSON. */
    given Decoder[Text] = deriveDecoder

  /**
   * A name token.
   *
   * @param name     The content of this name token.
   * @param category The category of this name token.
   */
  case class Name(name: String, category: Name.Category) extends Token

  /**
   * Factory for name tokens.
   */
  object Name extends ((String, Name.Category) => Name) :

    /** The given name token ordering. */
    given Ordering[Name] = (x, y) => compareOrElse(x.name, y.name, Ordering[Category].compare(x.category, y.category))

    /** The given encoding of name tokens to JSON. */
    given Encoder[Name] = deriveEncoder

    /** The given decoding of name tokens from JSON. */
    given Decoder[Name] = deriveDecoder

    /**
     * Definitions of the supported name categories.
     */
    enum Category:

      /** The supported name categories. */
      case Person, Organization, Location

    /**
     * Definitions associated with the defined name categories.
     */
    object Category:

      /** The index of name categories by lowercase string representation. */
      private[this] val categories = Category.values.map(category => category.toString.toLowerCase -> category).toMap

      /** The ordering of name categories. */
      given Ordering[Category] = _.ordinal - _.ordinal

      /** The encoding of name categories to JSON. */
      given Encoder[Category] = Encoder[String].contramap(_.toString)

      /** The decoding of name categories from JSON. */
      given Decoder[Category] = Decoder[String] emap { category =>
        categories get category.toLowerCase toRight Messages.invalidNameTokenCategory(category)
      }
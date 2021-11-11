/* Token.scala
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

/**
 * Base type for lore tokens.
 */
sealed trait Token

/**
 * Definitions associate with the token implementations.
 */
object Token:

  /** The given token ordering. */
  given Ordering[Token] = (x, y) => x match
    case Text(xString, xPartOfSpeech) => y match
      case Text(yString, yPartOfSpeech) => xString compareTo yString match
        case 0 => xPartOfSpeech.fold(yPartOfSpeech.fold(0)(_ => -1))(xpos => yPartOfSpeech.fold(1)(xpos.compareTo))
        case nonZero => nonZero
      case Name(yText, _) => xString compareTo yText match
        case 0 => -1
        case nonZero => nonZero
    case Name(xText, xCategory) => y match
      case Name(yText, yCategory) => xText compareTo yText match
        case 0 => xCategory.ordinal - yCategory.ordinal
        case nonZero => nonZero
      case Text(yString, _) => xText compareTo yString match
        case 0 => 1
        case nonZero => nonZero

  /**
   * A single piece of lore text.
   *
   * @param string The content of the lore text token.
   * @param partOfSpeech The part of speech this lore text token is tagged with.
   */
  case class Text(string: String, partOfSpeech: Option[String] = None) extends Token

  /**
   * A single lore name.
   *
   * @param text     The full text of this name.
   * @param category The category of this name.
   */
  case class Name(text: String, category: Name.Category) extends Token

  /**
   * Factory for single lore names.
   */
  object Name extends ((String, Name.Category) => Name) :

    /**
     * Definitions of the supported name categories.
     */
    enum Category :

      /** The supported name categories. */
      case Person, Organization, Location
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
 * Definitions of the token implementations.
 */
object Token:

  import cats.data.NonEmptyList
  import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}
  import io.circe.syntax.*

  /** The given token encoder. */
  given Encoder[Token] = _ match
    case t@Text(_, _) => Encoder[Text] apply t
    case n@Name(_) => Encoder[Name] apply n

  /** The given token decoder. */
  given Decoder[Token] = cursor =>
    Decoder[Text].apply(cursor) orElse Decoder[Name].apply(cursor)

  /**
   * Reports a failure to decode a token.
   *
   * @param cursor The cursor where the failure occurred.
   * @return A failure report when decoding a token.
   */
  private def decodingFailed(cursor: HCursor): Decoder.Result[Nothing] =
    Left(DecodingFailure(s"Failed to decode token.", cursor.history))

  /**
   * A single piece of lore text.
   *
   * @param content      The content of the lore text.
   * @param partOfSpeech The part of speech this lore text is tagged with.
   */
  case class Text(content: String, partOfSpeech: Option[String] = None) extends Token

  /**
   * Factoiry for single pieces of lore text.
   */
  object Text extends ((String, Option[String]) => Text) :

    /** The "txt" field name. */
    private val TXT = "txt"

    /** The "pos" field name. */
    private val POS = "pos"

    /** The text token encoder. */
    given Encoder[Text] = token =>
      token.partOfSpeech match
        case Some(pos) => Json.obj(TXT -> Json.fromString(token.content), POS -> Json.fromString(pos))
        case None => Json.obj(TXT -> Json.fromString(token.content))

    /** The text token decoder. */
    given Decoder[Text] = cursor =>
      cursor.downField(TXT).success match
        case Some(txt) =>
          cursor.downField(POS).success match
            case Some(pos) =>
              for
                _txt <- Decoder.decodeString(txt)
                _pos <- Decoder.decodeString(pos)
              yield Text(_txt, Some(_pos))
            case None =>
              Decoder.decodeString(txt) map (Text(_))
        case None => decodingFailed(cursor)

  /**
   * A single lore name.
   *
   * @param tokens The non-empty list of text tokens that define this name.
   */
  case class Name(tokens: NonEmptyList[Text]) extends Token

  /**
   * Factoiry for single lore names.
   */
  object Name extends (NonEmptyList[Text] => Name) :

    /** The "name" field name. */
    private val NAME = "name"

    /** The name token encoder. */
    given Encoder[Name] = token =>
      Json.obj(NAME -> Json.fromValues(token.tokens.iterator map (_.asJson) to Iterable))

    /** The name token decoder. */
    given Decoder[Name] = cursor =>
      cursor.downField(NAME).success match
        case Some(name) =>
          for
            _name <- Decoder.decodeList[Text].apply(name)
            result <- NonEmptyList.fromList(_name).fold(decodingFailed(cursor))(Right apply Name(_))
          yield result
        case None => decodingFailed(cursor)
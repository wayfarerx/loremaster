/* Tokens.scala
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

import io.circe.{ACursor, Decoder, Json}

import model.*

/**
 * Implementation of the token JSON codec.
 */
private case object Tokens extends Codec[Token] :

  /** The text string key. */
  private val STRING = "str"

  /** The text part of speech key. */
  private val PART_OF_SPEECH = "pos"

  /** The name text key. */
  private val TEXT = "txt"

  /** The name category key. */
  private val CATEGORY = "cat"

  /** The token name categories indexed by their lower-case representation. */
  private val Categories = Token.Name.Category.values.iterator.map(c => c.toString.toLowerCase -> c).toMap

  /* The type of this codec. */
  override val `type`: String = "Token"

  /* Encode the specified data into JSON. */
  override def encode(token: Token): Json = token match
    case text: Token.Text =>
      val withString = Json obj STRING -> Json.fromString(text.text)
      text.pos.fold(withString)(pos => withString mapObject (_.add(PART_OF_SPEECH, Json fromString pos)))
    case name: Token.Name => Json.obj(
      TEXT -> Json.fromString(name.name),
      CATEGORY -> Json.fromString(name.category.toString.toLowerCase)
    )

  /* Decode data from the specified JSON. */
  override def decode(cursor: ACursor): Decoder.Result[Token] = {
    val stringCursor = cursor.downField(STRING)
    val partOfSpeechCursor = cursor.downField(PART_OF_SPEECH)
    for
      string <- Strings decode stringCursor
      partOfSpeech <-
        if partOfSpeechCursor.succeeded then Strings decode partOfSpeechCursor map (Some(_))
        else Right(None)
    yield Token.Text(string, partOfSpeech)
  } orElse {
    val textCursor = cursor.downField(TEXT)
    val categoryCursor = cursor.downField(CATEGORY)
    for
      text <- Strings decode textCursor
      category <- Strings decode categoryCursor map (_.toLowerCase)
      _category <- Categories.get(category).fold(decodeFailed(categoryCursor))(Right(_))
    yield Token.Name(text, _category)
  } orElse
    decodeFailed(cursor)
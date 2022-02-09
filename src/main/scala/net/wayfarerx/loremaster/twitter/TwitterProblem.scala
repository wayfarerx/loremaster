/* TwitterProblem.scala
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
package twitter

import io.circe.{Decoder, Encoder, Json}
import Json.{arr, fromString, obj}

/**
 * A problem returned by Twitter.
 *
 * @param title    The title of this problem.
 * @param detail   The detail of this problem.
 * @param _type    The type of this problem.
 * @param messages The messages that describe this problem.
 */
case class TwitterProblem(title: String, detail: String, _type: String, messages: List[String] = Nil)

/**
 * A factory for problems returned by Twitter.
 */
object TwitterProblem extends ((String, String, String, List[String]) => TwitterProblem) :

  /** The name of the "title" field. */
  private[this] val Title = "title"

  /** The name of the "detail" field. */
  private[this] val Detail = "detail"

  /** The name of the "type" field. */
  private[this] val Type = "type"

  /** The name of the "errors" field. */
  private[this] val Errors = "errors"

  /** The name of the "message" field. */
  private[this] val Message = "message"

  /** The given JSON encoder for Twitter problems. */
  given Encoder[TwitterProblem] = problem => obj(
    Title -> fromString(problem.title),
    Detail -> fromString(problem.detail),
    Type -> fromString(problem._type),
    Errors -> arr(problem.messages map (message => obj(Message -> fromString(message))) *)
  )

  /** The given JSON decoder for Twitter problems. */
  given Decoder[TwitterProblem] = cursor => for
    title <- cursor.downField(Title).as[String]
    detail <- cursor.downField(Detail).as[String]
    _type <- cursor.downField(Type).as[String]
    messages <- cursor.downField(Errors).values.fold(Right(Nil))(errors => collectMessages(errors.toList))
  yield TwitterProblem(title, detail, _type, messages)

  /**
   * Collects the messages from a list of Twitter error JSON elements.
   *
   * @param errors The list of Twitter error JSON elements to collect from.
   * @return The messages collected from the list of Twitter error JSON elements.
   */
  private[this] def collectMessages(errors: List[Json]): Decoder.Result[List[String]] =
    errors match
      case Nil =>
        Right(Nil)
      case head :: tail =>
        head.asObject.flatMap(_ (Message)).fold(collectMessages(tail)) {
          _.as[String] flatMap (msg => collectMessages(tail) map (msg :: _))
        }

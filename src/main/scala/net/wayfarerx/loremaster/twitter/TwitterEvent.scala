/* TwitterEvent.scala
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

import java.time.Instant

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import event.*
import model.*

/**
 * Definition of a tweet event.
 *
 * @param book      The book that should be tweeted.
 * @param createdAt The instant that this event was crated at.
 * @param retry     The retry count for this event.
 */
case class TwitterEvent(book: Book, createdAt: Instant = Instant.now, retry: Option[Int] = None)

/**
 * Factory for tweet events.
 */
object TwitterEvent extends ((Book, Instant, Option[Int]) => TwitterEvent) :

  /** The encoding of tweet events to JSON. */
  given Encoder[TwitterEvent] = deriveEncoder[TwitterEvent].mapJson(_.dropNullValues)

  /** The decoding of tweet events from JSON. */
  given Decoder[TwitterEvent] = deriveDecoder

  /** Support for retrying tweet events. */
  given Event[TwitterEvent] = new Event[TwitterEvent] :

    /* Return the instant the event was originally created at. */
    override def createdAt(event: TwitterEvent): Instant = event.createdAt

    /* Return the number of times the event has been previously attempted. */
    override def previousAttempts(event: TwitterEvent): Int = event.retry.fold(0)(math.max(0, _))

    /* Return the specified event with its retry count incremented. */
    override def nextAttempt(event: TwitterEvent): TwitterEvent = event.copy(retry = Some(previousAttempts(event) + 1))

  /** The Twitter event topic variable name. */
  val Topic: String = "TwitterEvents"

  def main(args: Array[String]): Unit = println {
    cats.data.NonEmptyList.fromList(args.toList).fold("") { input =>
      Encoder[TwitterEvent].apply(TwitterEvent(Book(input))).spaces2
    }
  }
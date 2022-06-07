/* ComposerEvent.scala
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
package composer

import java.time.Instant

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import event.*

/**
 * Definition of a composer event.
 *
 * @param composition The composition specification to follow.
 * @param createdAt   The instant that this event was crated at.
 * @param retry       The retry count for this event.
 */
case class ComposerEvent(composition: Composition, createdAt: Instant = Instant.now, retry: Option[Int] = None)

/**
 * Factory for composer events.
 */
object ComposerEvent extends ((Composition, Instant, Option[Int]) => ComposerEvent) :

  /** The encoding of composer events to JSON. */
  given Encoder[ComposerEvent] = deriveEncoder[ComposerEvent].mapJson(_.dropNullValues)

  /** The decoding of composer events from JSON. */
  given Decoder[ComposerEvent] = deriveDecoder

  /** Support for retrying composer events. */
  given Event[ComposerEvent] = new Event[ComposerEvent] :

    /* Return the instant the event was originally created at. */
    override def createdAt(event: ComposerEvent): Instant =
      event.createdAt

    /* Return the number of times the event has been previously attempted. */
    override def previousAttempts(event: ComposerEvent): Int =
      event.retry.fold(0)(math.max(0, _))

    /* Return the specified event with its retry count incremented. */
    override def nextAttempt(event: ComposerEvent): ComposerEvent =
      event.copy(retry = Option(event.retry.fold(1)(Math.max(0, _) + 1)))
/* Event.scala
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
package event

import java.time.Instant

/**
 * A type class that describes events.
 *
 * @tparam T The type that is an event.
 */
trait Event[T]:

  /**
   * Returns the instant the event was originally created at.
   *
   * @param event The event to return the creation instant of.
   * @return The instant the event was originally created at.
   */
  def createdAt(event: T): Instant

  /**
   * Returns the number of times the event has previously been attempted.
   *
   * @param event The event to return the number of times it has previously been attempted.
   * @return The number of times the event has previously been attempted.
   */
  def previousAttempts(event: T): Int

  /**
   * Increments the attempt count for the specified event.
   *
   * @param event The event to increment the attempt count of.
   * @return The specified event with its attempt count incremented.
   */
  def nextAttempt(event: T): T

/**
 * Factory for event type classes.
 */
object Event:

  /**
   * Returns the given event type class for the specified type.
   *
   * @tparam T The type of event to use.
   * @return The given event type class for the specified type.
   */
  inline def apply[T: Event]: Event[T] = summon[Event[T]]
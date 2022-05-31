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

import scala.concurrent.duration.*

/**
 * Definition of the event publisher API.
 */
trait Publisher[T] extends ((T, Option[FiniteDuration]) => EventEffect[Unit]) :

  /**
   * Schedules an event for publishing.
   *
   * @param event The event to publish.
   * @return The result of scheduling an event for publishing.
   */
  final def apply(event: T): EventEffect[Unit] = apply(event, None)

  /**
   * Schedules an event for publishing.
   *
   * @param event The event to publish.
   * @param delay The delay to await before publishing.
   * @return The result of scheduling an event for publishing.
   */
  final def apply(event: T, delay: FiniteDuration): EventEffect[Unit] = apply(event, Option(delay))
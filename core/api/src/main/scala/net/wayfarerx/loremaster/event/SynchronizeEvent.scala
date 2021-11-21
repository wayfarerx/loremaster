/* SynchronizeEvent.scala
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
package event

import model.*

/**
 * Base type for synchronize events.
 */
sealed trait SynchronizeEvent

/**
 * Definitions of the supported synchronize events.
 */
object SynchronizeEvent:

  /**
   * An event that indicates an update should occur.
   */
  case object Update extends SynchronizeEvent

  /**
   * An event that indicates the specified entry should be mirrored.
   *
   * @param id The ID of the entry to mirror.
   * @param location The location of the entry to mirror.
   */
  case class Mirror(id: ID, location: Location) extends SynchronizeEvent
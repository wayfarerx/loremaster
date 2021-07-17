/* Configuration.scala
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

/**
 * Definition of the configuration service.
 */
trait Configuration:

  /** The path or URL to use for storing persistant data. */
  def storage: String

  /** The library that serves as the source of truth. */
  def library: String

/**
 * Definitions associated with configurations.
 */
object Configuration:

  import zio.{Has, URIO}

  /** Returns the path or URL to use for storing persistant data. */
  val storage: URIO[Has[Configuration], String] = URIO.service map (_.storage)

  /** Returns the library that serves as the source of truth. */
  val library: URIO[Has[Configuration], String] = URIO.service map (_.library)
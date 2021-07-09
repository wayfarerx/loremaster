/* Http.scala
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

import java.net.URI
import java.time.Instant
import zio.Task

/**
 * Definition of the http service API.
 */
trait Http {

  import Http.State

  /**
   * Downloads the content at the specified URI if it exists.
   *
   * @param uri The URI of the content to download.
   * @param ifModifiedAfter The optional constraint on when the content was last modified.
   * @return The content at the specified URI URI if it exists.
   */
  def get(uri: URI, ifModifiedAfter: Option[Instant] = None): Task[State]

  /**
   * Downloads the content at the specified URI if it exists.
   *
   * @param uri The URI of the content to download.
   * @param ifModifiedAfter The constraint on when the content was last modified.
   * @return The content at the specified URI URI if it exists.
   */
  inline final def get(uri: URI, ifModifiedAfter: Instant): Task[State] = get(uri, Some(ifModifiedAfter))

}

object Http:

  import zio.{Has, RIO}

  /**
   * Downloads the content at the specified URI if it exists.
   *
   * @param uri The URI of the content to download.
   * @param ifModifiedAfter The optional constraint on when the content was last modified.
   * @return The content at the specified URI URI if it exists.
   */
  inline def get(uri: URI, ifModifiedAfter: Option[Instant] = None): RIO[Has[Http], State] =
    RIO.service flatMap (_.get(uri, ifModifiedAfter))

  /**
   * Downloads the content at the specified URI if it exists.
   *
   * @param uri The URI of the content to download.
   * @param ifModifiedAfter The constraint on when the content was last modified.
   * @return The content at the specified URI URI if it exists.
   */
  inline def get(uri: URI, ifModifiedAfter: Instant): RIO[Has[Http], State] =
    RIO.service flatMap (_.get(uri, ifModifiedAfter))

  sealed trait State

  object State:

    case object NotFound extends State

    case object NotModified extends State

    case class Latest(content: String) extends State

/* TwitterClient.scala
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

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import zio.IO

import model.*

/**
 * Definition of a Twitter client.
 */
trait TwitterClient:

  /**
   * Posts a book to Twitter.
   *
   * @param book The book to post to Twitter.
   * @return The result of posting a book to Twitter.
   */
  def postTweet(book: Book): TwitterEffect[Unit]
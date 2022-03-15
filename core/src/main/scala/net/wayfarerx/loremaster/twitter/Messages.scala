/* Messages.scala
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

import scala.concurrent.duration.*

/**
 * The messages provided by the twitter package.
 */
private object Messages:

  def beforeTwitterEvent: String =
    "Before Twitter event"

  def afterTwitterEvent: String =
    "After Twitter event"

  def twitterFailure(message: String): String =
    s"Twitter failure: $message"

  def tweeted(event: TwitterEvent): String =
    s"Tweeted: ${format(event)}"

  def retryingTweet(event: TwitterEvent, delay: FiniteDuration) =
    s"Retrying tweet after $delay: ${format(event)}"

  def failedToTweet(event: TwitterEvent): String =
    s"Failed to tweet: ${format(event)}"

  private[this] def format(event: TwitterEvent): String =
    event.book.paragraphs.iterator mkString " "
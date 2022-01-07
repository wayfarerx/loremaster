/* Messages.scala
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
package twitter

/**
 * The messages provided by the AWS package.
 */
private object Messages {

  def beforeTweeting: String = "Before tweeting"

  def afterTweeting: String = "After tweeting"

  def postingTweet: String = "Posting tweet"

  def postedTweet: String = "Posted tweet"

  def retryingTweet: String = "Retrying failed tweet"

  def retriedTweet: String = "Retried failed tweet"

  def networkUnavailable: String = "Cannot to connect to Twitter"

  def rateLimitExceeded: String = "Twitter rate limit exceeded"

  def unexpectedFailure: String = "Unexpected Twitter failure"

  def failedToTweet(event: TweetEvent): String = s"Failed to tweet: ${event.book.paragraphs.iterator mkString " "}"

}

/* TwitterService.scala
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

import zio.Task

import event.*
import logging.*

/**
 * Definition of the tweet service API.
 */
trait TwitterService extends (TwitterEvent => Task[Unit])

/**
 * Definitions associated with Twitter services.
 */
object TwitterService extends ((Log, Retries, TwitterClient, Publisher[TwitterEvent]) => TwitterService) :

  /**
   * Creates an implementation of the Twitter service.
   *
   * @param log       The log to use.
   * @param retries   The retry policy to use.
   * @param client    The Twitter client to use.
   * @param publisher The tweet publisher to retry with.
   * @return An implementation of the Twitter service.
   */
  override def apply(
    log: Log,
    retries: Retries,
    client: TwitterClient,
    publisher: Publisher[TwitterEvent]
  ): TwitterService = event =>
    (client.postTweet(event.book) *> log.info(Messages.tweeted(event))).catchSome {
      case problem if problem.shouldRetry =>
        retries(event).fold(Task.fail(problem)) { backoff =>
          log.warn(Messages.retryingTweet(event, backoff)) *> publisher(event.next, backoff)
        }
    }
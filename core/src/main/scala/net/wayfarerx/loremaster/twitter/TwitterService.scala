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

import zio.{Task, UIO}

import event.*
import http.*
import logging.*

/**
 * Definition of the tweet service API.
 */
trait TwitterService extends (TwitterEvent => UIO[Unit])

/**
 * Definitions associated with Twitter services.
 */
object TwitterService extends ((Retries, Log, TwitterClient, Publisher[TwitterEvent]) => TwitterService) :

  /**
   * Creates an implementation of the Twitter service.
   *
   * @param retries   The retry policy to use.
   * @param log       The log to use.
   * @param client    The Twitter client to use.
   * @param publisher The tweet publisher to retry with.
   * @return An implementation of the Twitter service.
   */
  override def apply(
    retries: Retries,
    log: Log,
    client: TwitterClient,
    publisher: Publisher[TwitterEvent]
  ): TwitterService = event => for
    _ <- log.trace(Messages.beforeTwitterEvent)
    _ <- (client.postTweet(event.book) *> log.info(Messages.tweeted(event))).catchSome {
      case problem if problem.shouldRetry =>
        retries(event).fold(Task.fail(problem)) { delay =>
          log.warn(Messages.retryingTweet(event, delay)) *> publisher(event.next, delay)
        }
    }.catchAll(log.error(Messages.failedToTweet(event), _))
    _ <- log.trace(Messages.afterTwitterEvent)
  yield ()
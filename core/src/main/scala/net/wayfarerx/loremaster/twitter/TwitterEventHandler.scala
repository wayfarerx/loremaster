/* TwitterEventHandler.scala
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

import zio.IO

import event.*
import logging.*

/**
 * Definition of the Twitter event handler.
 *
 * @param log         The log to use.
 * @param retryPolicy The retry policy to use.
 * @param client      The Twitter client to use.
 * @param fallback    The Twitter event publisher to retry with.
 */
final class TwitterEventHandler(
  log: Log,
  retryPolicy: RetryPolicy,
  client: TwitterClient,
  fallback: Publisher[TwitterEvent]
) extends (TwitterEvent => TwitterEffect[Unit]) :

  /* Handle a Twitter event. */
  override def apply(event: TwitterEvent): TwitterEffect[Unit] = {
    client.postTweet(event.book) *> log.info(Messages.tweeted(event))
  } catchSome {
    case problem if problem.shouldRetry =>
      retryPolicy(event).fold(IO.fail(problem)) { backoff =>
        log.warn(Messages.retryingTweet(event, backoff)) *>
          fallback(event.copy(retry = Some(event.retry.fold(1)(Math.max(0, _) + 1))), backoff) catchAll { thrown =>
          IO.fail(TwitterProblem(Messages.failedToRetryTweet(event), Option(thrown)))
        }
      }
  }
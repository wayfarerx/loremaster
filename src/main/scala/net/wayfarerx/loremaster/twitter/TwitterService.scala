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
import logging.*

/**
 * Definition of the tweet service API.
 */
trait TwitterService extends (TwitterEvent => UIO[Unit])

/**
 * Definitions associated with Twitter services.
 */
object TwitterService
  extends ((TwitterConfiguration, LogFactory, TwitterClient, Publisher[TwitterEvent]) => Task[TwitterService]) :

  /**
   * Creates an implementation of the Twitter service.
   *
   * @param config     The Twitter configuration to use.
   * @param logFactory The log factory to use.
   * @param client     The Twitter client to use.
   * @param publisher  The tweet publisher to retry with.
   * @return An implementation of the Twitter service.
   */
  override def apply(
    config: TwitterConfiguration,
    logFactory: LogFactory,
    client: TwitterClient,
    publisher: Publisher[TwitterEvent]
  ): Task[TwitterService] =
    logFactory.log[TwitterService].map(Live(_, client, publisher))

  /**
   * A live implementation of the Twitter service.
   *
   * @param log       The log to use.
   * @param client    The Twitter client to use.
   * @param publisher The Twitter event publisher to use.
   */
  private final class Live(
    log: Log,
    client: TwitterClient,
    publisher: Publisher[TwitterEvent]
  ) extends TwitterService :

    /**
     * Handles a tweet event.
     *
     * @param event The tweet event to handle.
     * @return The result of handling the specified tweet event.
     */
    override def apply(event: TwitterEvent): UIO[Unit] = for
      _ <- log.trace(Messages.beforeTwitterEvent)
      _ <- client(event).flatMap {
        _.fold(log.info(Messages.tweeted(event))) { case (next, delay) =>
          log.debug(Messages.retryingTweet(event, delay)) *> publisher(next, delay)
        }
      }.catchAll(log.error(Messages.failedToTweet(event), _))
      _ <- log.trace(Messages.afterTwitterEvent)
    yield ()
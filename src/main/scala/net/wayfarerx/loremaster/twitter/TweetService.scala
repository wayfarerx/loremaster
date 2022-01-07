/* TweetService.scala
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

import scala.concurrent.duration.*

import twitter4j.{Twitter, TwitterException}

import zio.{Task, UIO}

import configuration.*
import event.*
import logging.*

/**
 * Definition of the tweet service API.
 */
trait TweetService extends (TweetEvent => UIO[Unit])

/**
 * Definitions associated with tweet services.
 */
object TweetService extends ((Configuration, LogFactory, Twitter, Publisher[TweetEvent]) => Task[TweetService]) :

  /**
   * Creates an implementation of the tweet service API.
   *
   * @param config     The configuration to use.
   * @param logFactory The log factory to use.
   * @param twitter    The connection to Twitter.
   * @param publisher  The tweet publisher to retry with.
   * @return An implementation of the tweet service API.
   */
  override def apply(
    config: Configuration,
    logFactory: LogFactory,
    twitter: Twitter,
    publisher: Publisher[TweetEvent]
  ): Task[TweetService] = for
    retries <- config.getOrElse(s"$TwitterPrefix.${Retries.Name}", Retries.Default)
    log <- logFactory.log[TweetService]
    _ <- log.debug(s"${Retries.Name} = $retries")
  yield Live(log, retries, twitter, publisher)

  /**
   * A live implementation of the tweet service.
   *
   * @param log       The log to use.
   * @param retries   The retry policy to use.
   * @param twitter   The Twitter interface to use.
   * @param publisher The tweet event publisher to use.
   */
  private final class Live(
    log: Log,
    retries: Retries,
    twitter: Twitter,
    publisher: Publisher[TweetEvent]
  ) extends TweetService :

    /**
     * Handles a tweet event.
     *
     * @param event The tweet event to handle.
     * @return The result of handling the specified tweet event.
     */
    override def apply(event: TweetEvent): UIO[Unit] =
      log.trace(Messages.beforeTweeting) *> {
        postTweet(event) *> UIO.none catchSome {
          case thrown: TwitterException => {
            if thrown.isCausedByNetworkIssue then log.warn(Messages.networkUnavailable, thrown)
            else if thrown.exceededRateLimitation then log.warn(Messages.rateLimitExceeded, thrown)
            else log.warn(Messages.unexpectedFailure, thrown)
          } flatMap (_ => retries(event).fold(log.error(Messages failedToTweet event) *> UIO.none)(UIO.some))
        } flatMap (_.fold(UIO.unit) { case (next, delay) => retryTweet(next, delay) })
      }.catchAll(log.error(Messages failedToTweet event, _)) *>
        log.trace(Messages.afterTweeting)

    /**
     * Posts a tweet event to Twitter.
     *
     * @param event The tweet event to post to Twitter.
     * @return The result of posting a tweet event to Twitter.
     */
    private[this] def postTweet(event: TweetEvent): Task[Unit] =
      log.info(Messages.postingTweet) *>
        Task(twitter.updateStatus(event.book.toString)) *>
        log.info(Messages.postedTweet)

    /**
     * Retries posting a tweet event to Twitter.
     *
     * @param next The tweet event to retry posting to Twitter.
     * @param delay The delay that should elapse before retrying.
     * @return The result of retrying to post a tweet event to Twitter.
     */
    private[this] def retryTweet(next: TweetEvent, delay: FiniteDuration): Task[Unit] =
      log.info(Messages.retryingTweet) *>
        publisher(next, delay) *>
        log.info(Messages.retriedTweet)
/* TweetPublisher.scala
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

import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder

import zio.Task

import model.*
import logging.*

/**
 * Definition of the tweet publisher API.
 */
trait TweetPublisher extends (Book => Task[Unit])

/**
 * Definitions associated with tweet publishers.
 */
object TweetPublisher extends ((TwitterCredentials, LogFactory) => Task[TweetPublisher]) :

  /**
   * Creates an implementation of the tweet publisher API.
   *
   * @param credentials The credentials to use when authenticating with Twitter.
   * @param logFactory  The log factory to use.
   * @return An implementation of the tweet publisher API.
   */
  override def apply(credentials: TwitterCredentials, logFactory: LogFactory): Task[TweetPublisher] = for
    log <- logFactory.log[TweetPublisher]
    _ <- log.trace(s"Before creating $Twitter connection")
    connection <- Task apply TwitterFactory(ConfigurationBuilder()
      .setOAuthConsumerKey(credentials.consumerKey)
      .setOAuthConsumerSecret(credentials.consumerSecret)
      .setOAuthAccessToken(credentials.accessToken)
      .setOAuthAccessTokenSecret(credentials.accessTokenSecret)
      .build
    ).getInstance
    _ <- log.trace(s"After creating $Twitter connection")
  yield book => for
    _ <- log.trace("Before publishing tweet")
    _ <- Task(connection.updateStatus(book.paragraphs.iterator mkString "\r\n" * 2))
    _ <- log.trace("After publishing tweet")
  yield ()
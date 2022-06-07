/* TwitterConnection.scala
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
package deployment

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

import twitter4j.{Twitter, TwitterException, TwitterFactory}
import twitter4j.conf.ConfigurationBuilder

import zio.{IO, Task}

import configuration.*
import model.*


/**
 * Exposes a connection to Twitter as a client.
 *
 * @param connection The Twitter connection to expose as a client.
 */
final class TwitterConnection(connection: Twitter) extends TwitterClient :

  /* Post a book to Twitter. */
  override def postTweet(book: Book): IO[TwitterProblem, Unit] = Task {
    connection.updateStatus(book.toString)
  } *> Task.unit catchAll {
    case thrown: TwitterException => IO.fail(
      TwitterProblem(
        Messages.twitterFailure(thrown.getStatusCode),
        Option(thrown),
        thrown.getStatusCode == 429 || thrown.getStatusCode >= 500
      )
    )
    case NonFatal(nonFatal) => IO.fail(TwitterProblem(Messages.twitterError, Option(nonFatal)))
    case fatal => IO.die(fatal)
  }

/**
 * Factory for Twitter connections.
 */
object TwitterConnection extends (Twitter => TwitterConnection) :

  /**
   * Creates a Twitter connection.
   *
   * @param connection The connection to wrap.
   * @return A new Twitter connection.
   */
  override def apply(connection: Twitter): TwitterConnection = new TwitterConnection(connection)

  /**
   * Configures a new Twitter connection.
   *
   * @param config The configuration to use.
   * @return A new Twitter connection.
   */
  def configure(config: Configuration): Task[TwitterConnection] = for
    consumerKey <- config[String](TwitterConsumerKey)
    consumerSecret <- config[String](TwitterConsumerSecret)
    accessToken <- config[String](TwitterAccessToken)
    accessTokenSecret <- config[String](TwitterAccessTokenSecret)
    connectionTimeout <- config[FiniteDuration](TwitterConnectionTimeout)
    result <- Task {
      apply(
        TwitterFactory(
          ConfigurationBuilder()
            .setOAuthConsumerKey(consumerKey)
            .setOAuthConsumerSecret(consumerSecret)
            .setOAuthAccessToken(accessToken)
            .setOAuthAccessTokenSecret(accessTokenSecret)
            .setHttpConnectionTimeout(connectionTimeout.toMillis.toInt)
            .build
        ).getInstance
      )
    }
  yield result
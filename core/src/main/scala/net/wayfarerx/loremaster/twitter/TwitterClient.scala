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

import twitter4j.{Twitter, TwitterException, TwitterFactory}
import twitter4j.conf.ConfigurationBuilder

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
  def postTweet(book: Book): IO[TwitterProblem, Unit]

/**
 * Factory for Twitter clients.
 */
object TwitterClient extends ((String, String, String, String, FiniteDuration) => TwitterClient) :

  /**
   * Creates a Twitter client.
   *
   * @param consumerKey       The consumer key to authenticate to Twitter with.
   * @param consumerSecret    The consumer secret to authenticate to Twitter with.
   * @param accessToken       The access token to authenticate to Twitter with.
   * @param accessTokenSecret The access token secret to authenticate to Twitter with.
   * @param connectionTimeout The timeout to use for Twitter connections.
   * @return A new Twitter client.
   */
  override def apply(
    consumerKey: String,
    consumerSecret: String,
    accessToken: String,
    accessTokenSecret: String,
    connectionTimeout: FiniteDuration
  ): TwitterClient = apply(
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

  /**
   * Creates a Twitter client.
   *
   * @param connection The connection to Twitter.
   * @return A new Twitter client.
   */
  def apply(connection: Twitter): TwitterClient = book =>
    IO(connection.updateStatus(book.toString)) catchAll {
      case thrown: TwitterException => IO.fail(
        TwitterProblem(
          Messages.twitterFailure(thrown.getMessage),
          Some(thrown),
          thrown.getStatusCode == 429 || thrown.getStatusCode >= 500
        )
      )
      case NonFatal(nonFatal) =>
        IO.fail(TwitterProblem(Messages.twitterError(nonFatal.getMessage), Some(nonFatal)))
      case fatal =>
        IO.die(fatal)
    } map (_ => ())
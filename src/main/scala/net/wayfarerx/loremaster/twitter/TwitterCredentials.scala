/* TwitterCredentials.scala
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

import twitter4j.conf.ConfigurationBuilder

import zio.Task

import configuration.*

/**
 * OAuth credentials that can be used to authenticate with Twitter.
 *
 * @param consumerKey       The OAuth consumer key to authenticate with.
 * @param consumerSecret    The OAuth consumer secret to authenticate with.
 * @param accessToken       The OAuth access token to authenticate with.
 * @param accessTokenSecret The OAuth access token secret to authenticate with.
 */
case class TwitterCredentials(
  consumerKey: String,
  consumerSecret: String,
  accessToken: String,
  accessTokenSecret: String
):

  /**
   * Configures a Twitter connection with these credentials.
   *
   * @param builder The configuration builder to use.
   * @return The specified configuration builder with these credentials added.
   */
  def configure(builder: ConfigurationBuilder): ConfigurationBuilder = builder
    .setOAuthConsumerKey(consumerKey)
    .setOAuthConsumerSecret(consumerSecret)
    .setOAuthAccessToken(accessToken)
    .setOAuthAccessTokenSecret(accessTokenSecret)

/**
 * Factory for Twitter credentials.
 */
object TwitterCredentials extends ((String, String, String, String) => TwitterCredentials) :

  /**
   * Creates configured Twitter credentials.
   *
   * @param config The configuration to use.
   * @return The configured Twitter credentials.
   */
  def apply(config: Configuration): Task[TwitterCredentials] = for
    consumerKey <- config[String](s"$TwitterPrefix.consumerKey")
    consumerSecret <- config[String](s"$TwitterPrefix.consumerSecret")
    accessToken <- config[String](s"$TwitterPrefix.accessToken")
    accessTokenSecret <- config[String](s"$TwitterPrefix.accessTokenSecret")
  yield TwitterCredentials(consumerKey, consumerSecret, accessToken, accessTokenSecret)
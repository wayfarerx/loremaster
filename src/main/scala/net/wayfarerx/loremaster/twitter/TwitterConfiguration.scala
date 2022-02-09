/* TwitterConfiguration.scala
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

import scala.concurrent.duration.*

import zio.Task

import configuration.*
import event.*

/**
 * Settings that can be used to connect to Twitter.
 *
 * @param eventTopic        The Twitter event topic name.
 * @param bearerToken       The bearer token to authenticate to Twitter with.
 * @param connectionTimeout The optional timeout to use when connecting to Twitter.
 * @param retryPolicy       The optional retry policy to use when interacting with Twitter.
 */
case class TwitterConfiguration(
  eventTopic: String,
  bearerToken: String,
  connectionTimeout: Option[FiniteDuration] = None,
  retryPolicy: Option[Retries] = None
)

/**
 * Factory for Twitter credentials.
 */
object TwitterConfiguration
  extends ((String, String, Option[FiniteDuration], Option[Retries]) => TwitterConfiguration) :

  /** The Twitter event topic variable name. */
  val EventTopic = s"${Twitter}EventTopic"

  /** The Twitter bearer token variable name. */
  val BearerToken: String = s"${Twitter}BearerToken"

  /** The Twitter connection timeout variable name. */
  val ConnectionTimeout: String = s"${Twitter}ConnectionTimeout"

  /** The Twitter retry policy variable name. */
  val RetryPolicy = s"${Twitter}RetryPolicy"

  /**
   * Creates configured Twitter credentials.
   *
   * @param config The configuration to use.
   * @return The configured Twitter credentials.
   */
  def apply(config: Configuration): Task[TwitterConfiguration] = for
    eventTopic <- config[String](EventTopic)
    bearerToken <- config[String](BearerToken)
    connectionTimeout <- config.get[FiniteDuration](ConnectionTimeout)
    retryPolicy <- config.get[Retries](RetryPolicy)
  yield TwitterConfiguration(eventTopic, bearerToken, connectionTimeout, retryPolicy)
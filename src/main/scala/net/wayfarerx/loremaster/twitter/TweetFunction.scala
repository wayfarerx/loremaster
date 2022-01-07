/* TweetFunction.scala
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

import zio.{Has, RLayer, URIO, Task, ZLayer}

import aws.*
import configuration.*
import logging.*

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TweetFunction extends SqsFunction[TweetFunction.Environment, TweetEvent](TweetFunction.Environment) :

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TweetEvent): URIO[TweetFunction.Environment, Unit] =
    URIO.service[TweetService] flatMap (_(event))

/**
 * Definitions associated with tweet functions.
 */
object TweetFunction:

  /** The environment a tweet function operates in. */
  type Environment = AwsEnv & Has[TweetService]

  /** A factory for tweet function environments. */
  val Environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect {
      for
        config <- URIO.service[Configuration]
        logFactory <- URIO.service[LogFactory]
        credentials <- TwitterCredentials(config)
        twitter <- Task(TwitterFactory(credentials.configure(ConfigurationBuilder()).build).getInstance)
        publisher <- SqsPublisher[TweetEvent](TweetEvent.Topic, config)
        service <- TweetService(config, logFactory, twitter, publisher)
      yield service
    }
/* TwitterFunction.scala
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

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent

import zio.{Has, RLayer, URIO, ZLayer}

import aws.*
import configuration.*
import event.*
import http.*
import logging.*

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TwitterFunction extends SqsFunction[TwitterEvent] with RequestHandler[SQSEvent, String] :

  /* The type of environment to use. */
  override protected type Environment = TwitterFunction.Environment

  /* The environment constructor to use. */
  override protected def environment: RLayer[AwsEnv, Environment] = TwitterFunction.Environment

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TwitterEvent): URIO[Environment, Unit] =
    URIO.service[TwitterService].flatMap(_ (event))

/**
 * Definitions associated with tweet functions.
 */
object TwitterFunction:

  /** The environment a tweet function operates in. */
  type Environment = AwsEnv & Has[TwitterService]

  /** A factory for tweet function environments. */
  val Environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect {
      for
        config <- URIO.service[Configuration].flatMap(TwitterConfiguration(_))
        log <- URIO.service[LogFactory].flatMap(_.log[TwitterService])
        client <- Http(config.connectionTimeout).map(TwitterClient(config.bearerToken, _))
        publisher <- SqsPublisher[TwitterEvent](config.queueName)
      yield TwitterService(config.retryPolicy getOrElse Retries.Default, log, client, publisher)
    }
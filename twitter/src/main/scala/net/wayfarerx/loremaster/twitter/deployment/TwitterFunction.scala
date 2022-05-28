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

import scala.concurrent.duration.FiniteDuration

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent

import zio.{Has, RIO, RLayer, ZLayer}

import aws.*
import configuration.*
import event.*
import logging.*

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TwitterFunction extends SqsFunction[TwitterEvent] with RequestHandler[SQSEvent, String] :

  /* The type of environment to use. */
  override type Environment = FunctionEnv & Has[TwitterService]

  /* The environment constructor to use. */
  override def environment: RLayer[FunctionEnv, Environment] =
    ZLayer.requires[FunctionEnv] ++ ZLayer.fromEffect {
      for
        logFactory <- RIO.service[LogFactory]
        log <- logFactory.log[TwitterService]
        config <- RIO.service[Configuration]
        retryPolicy <- config[RetryPolicy](TwitterRetryPolicy)
        consumerKey <- config[String](TwitterConsumerKey)
        consumerSecret <- config[String](TwitterConsumerSecret)
        accessToken <- config[String](TwitterAccessToken)
        accessTokenSecret <- config[String](TwitterAccessTokenSecret)
        connectionTimeout <- config[FiniteDuration](TwitterConnectionTimeout)
        queueName <- config[String](TwitterQueueName)
      yield TwitterService(
        log,
        retryPolicy,
        TwitterConnection(consumerKey, consumerSecret, accessToken, accessTokenSecret, connectionTimeout),
        SqsPublisher[TwitterEvent](queueName)
      )
    }

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TwitterEvent): RIO[Environment, Unit] =
    RIO.service[TwitterService].flatMap(_ (event))
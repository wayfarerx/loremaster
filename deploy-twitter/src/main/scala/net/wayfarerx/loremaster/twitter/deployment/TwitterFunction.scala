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
import com.amazonaws.services.sqs.AmazonSQSClientBuilder

import zio.{Has, RLayer, Task, URIO, ZLayer}

import configuration.*
import deploy.*
import event.*
import http.*
import logging.*

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TwitterFunction extends SqsFunction[TwitterEvent] :

  /* The type of environment to use. */
  override type Environment = AwsEnv & Has[TwitterService]

  /* The environment constructor to use. */
  override def environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect {
      for
        config <- URIO.service[Configuration]
        twitterConfig <- TwitterConfiguration(config)
        logFactory <- URIO.service[LogFactory]
        log <- logFactory.log[TwitterService]
        client <- Http(twitterConfig.connectionTimeout).map(TwitterClient(twitterConfig.bearerToken, _))
      yield TwitterService(
        twitterConfig.retryPolicy getOrElse Retries.Default,
        log,
        client,
        SqsPublisher[TwitterEvent](twitterConfig.queueName)
      )
    }

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TwitterEvent): URIO[Environment, Unit] =
    URIO.service[TwitterService].flatMap(_ (event))
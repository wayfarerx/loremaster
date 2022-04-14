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

import zio.{Has, RLayer, RIO, ZLayer}

import configuration.*
import deployments.*
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
        logFactory <- RIO.service[LogFactory]
        log <- logFactory.log[TwitterService]
        config <- RIO.service[Configuration]
        retries <- config[Retries](TwitterRetryPolicy)
        connectionTimeout <- config[FiniteDuration](TwitterConnectionTimeout)
        bearerToken <- config[String](TwitterBearerToken)
        client <- Http(connectionTimeout).map(TwitterClient(_, bearerToken))
        publisher <- config[String](TwitterQueueName).map(SqsPublisher[TwitterEvent](_))
      yield TwitterService(log, retries, client, publisher)
    }

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TwitterEvent): RIO[Environment, Unit] =
    RIO.service[TwitterService].flatMap(_ (event))
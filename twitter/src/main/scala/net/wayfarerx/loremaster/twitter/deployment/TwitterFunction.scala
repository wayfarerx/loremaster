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

import zio.{Has, RIO, RLayer, ZLayer}

import aws.*
import configuration.*
import event.*
import logging.*

/**
 * An AWS SQS Lambda function that posts books to Twitter.
 */
final class TwitterFunction extends SqsFunction[TwitterEvent] with RequestHandler[SQSEvent, String] :

  /* The type of environment to use. */
  override type Environment = AwsEnv & Has[TwitterEventHandler]

  /* The environment constructor to use. */
  override def environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect {
      for
        config <- RIO.service[Configuration]
        log <- RIO.service[Logging] flatMap (_.log[TwitterEventHandler])
        retryPolicy <- config[RetryPolicy](TwitterRetryPolicy)
        connection <- TwitterConnection.configure(config)
      yield TwitterEventHandler(log, retryPolicy, connection, SqsPublisher[TwitterEvent])
    }

  /* Publish the specified book to Twitter. */
  override protected def onMessage(event: TwitterEvent): RIO[Environment, Unit] =
    RIO.service[TwitterEventHandler] flatMap (_ (event))
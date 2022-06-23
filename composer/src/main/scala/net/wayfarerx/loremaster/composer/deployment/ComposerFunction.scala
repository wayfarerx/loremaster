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
package composer
package deployment

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent

import zio.{Has, RIO, RLayer, UIO, ZLayer}
import zio.random.Random

import aws.*
import configuration.*
import event.*
import logging.*
import twitter.*

import nlp.deployment.OpenNlpRenderer
import repository.deployment.MockRepository

/**
 * An AWS SQS Lambda function that composes books.
 */
final class ComposerFunction extends SqsFunction[ComposerEvent] :

  /* The type of environment to use. */
  override type Environment = AwsEnv & Has[ComposerEventHandler]

  /* The environment constructor to use. */
  override def environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect {
      for
        logging <- RIO.service[Logging]
        log <- logging.log[ComposerEventHandler]
        config <- RIO.service[Configuration]
        retryPolicy <- config[RetryPolicy](ComposerRetryPolicy)
        repository <- UIO(MockRepository) // FIXME Use a real repository.
        rng <- RIO.service[Random.Service]
        renderer <- OpenNlpRenderer.configure(ComposerDetokenizerDictionary, config)
      yield ComposerEventHandler(
        log,
        retryPolicy,
        repository,
        rng,
        renderer,
        SqsPublisher[TwitterEvent],
        SqsPublisher[ComposerEvent]
      )
    }

  /* Compose the specified book. */
  override protected def onMessage(event: ComposerEvent): RIO[EnvironmentWithLog, Unit] =
    RIO.service[ComposerEventHandler].flatMap(_ (event))
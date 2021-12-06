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

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.KinesisEvent

import zio.{Has, RIO, RLayer, Task, ZEnv, ZLayer}

import aws.*
import configuration.*
import logging.*
import model.*
import event.TweetEvent

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TweetFunction extends KinesisFunction[TweetFunction.Environment, TweetEvent](TweetFunction.Environment):

  /* Publish the specified book to Twitter. */
  override def apply(event: TweetEvent): RIO[TweetFunction.Environment, Unit] = for
    log <- RIO.service[Log]
    publisher <- RIO.service[TweetPublisher]
    _ <- log.trace("Before handling tweet event")
    _ <- publisher(event.book)
    _ <- log.trace("After handling tweet event")
  yield ()

/**
 * Definitions associated with tweet functions.
 */
object TweetFunction:

  /** The environment a tweet function operates in. */
  type Environment = ZEnv & Has[Log] & Has[TweetPublisher]

  /** A factory for tweet function environments. */
  private val Environment: RLayer[ZEnv & Has[Configuration] & Has[LogFactory], Environment] =
    val log = ZLayer fromEffect RIO.service[LogFactory].flatMap(_.log[TweetFunction])
    val publisher = ZLayer fromEffect {
      for
        config <- RIO.service[Configuration]
        credentials <- TwitterCredentials(config)
        logFactory <- RIO.service[LogFactory]
        result <- TweetPublisher(credentials, logFactory)
      yield result
    }
    ZLayer.requires[ZEnv] ++ log ++ publisher
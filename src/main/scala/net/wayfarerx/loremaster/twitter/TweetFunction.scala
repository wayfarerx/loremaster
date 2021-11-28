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

/**
 * An AWS Kinesis Lambda function that posts books to Twitter,
 */
final class TweetFunction extends KinesisFunction[TweetFunction.Environment, Book](TweetFunction.Environment):

  /* Publish the specified book to Twitter. */
  override def apply(event: Book): RIO[TweetFunction.Environment, Unit] = for
    log <- RIO.service[Log]
    publisher <- RIO.service[TweetPublisher]
    _ <- log.trace("Before handling tweet event")
    _ <- publisher(event)
    _ <- log.trace("After handling tweet event")
  yield ()

object TweetFunction:

  type Environment = ZEnv & Has[Log] & Has[TweetPublisher]

  private val Environment: RLayer[ZEnv & Has[Configuration] & Has[LogFactory], Environment] =
    val log = ZLayer fromEffect RIO.service[LogFactory].flatMap(_.log[TweetFunction])
    val credentials = ZLayer fromEffect {
      for
        config <- RIO.service[Configuration]
        result <- TwitterCredentials(config)
      yield result
    }

//    val credentials = ZLayer.requires[Has[Configuration]].flatMap {
//      in => ???
//    }
//    val publisher = ZLayer.fromEffect {
//      for
//        config <- RIO.service[Configuration]
//        logFactory <- RIO.service[LogFactory]
//        log <- logFactory.log[TweetFunction]
//        credentials <- config >>> TwitterCredentials.live
//        publisher <- TweetPublisher(???, logFactory)
//      yield ???
//    }
//    ZLayer.requires[ZEnv] ++ publisher
    ???

/* SqsFunction.scala
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
package aws

import scala.jdk.CollectionConverters.*


import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent

import io.circe.Decoder

import zio.{RIO, URIO}

import logging.*

/**
 * Base type for SQS Lambda functions that operate in the specified environment.
 *
 * @tparam T The type of message this SQS function handles.
 */
trait SqsFunction[T: Decoder] extends LambdaFunction[SQSEvent] :
  self: RequestHandler[SQSEvent, String] =>

  /* Process a SQS event. */
  final override def apply(event: SQSEvent): RIO[Environment, Unit] = for
    logFactory <- RIO.service[LogFactory]
    log <- logFactory(getClass)
    _ <- onMessages(log, event.getRecords.asScala.toList)
  yield ()

  /**
   * Handles messages from the SQS queue.
   *
   * @param messages The messages to handle.
   * @return An effect that handles the specified messages.
   */
  private def onMessages(log: Log, messages: List[SQSEvent.SQSMessage]): URIO[Environment, Unit] = messages match
    case head :: tail =>
      for
        _ <- Option(head.getBody).filterNot(_.isEmpty).fold(URIO.unit) { message =>
          parse[T](message).fold(
            log.error(Messages.failedToParseSqsMessage(message), _),
            onMessage(_).catchAll(log.error(Messages.failedToDeliverSqsMessage(message), _))
          )
        }
        _ <- onMessages(log, tail)
      yield ()
    case Nil =>
      URIO.unit

  /**
   * Handles a message from the SQS queue.
   *
   * @param message The message to handle.
   * @return A task that handles the specified message.
   */
  protected def onMessage(message: T): RIO[Environment, Unit]

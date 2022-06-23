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

import zio.{Has, RIO}

import logging.*

/**
 * Base type for SQS Lambda functions.
 *
 * @tparam T The type of message this SQS function handles.
 */
trait SqsFunction[T: Decoder] extends LambdaFunction[SQSEvent] with RequestHandler[SQSEvent, String] :

  /* Process a SQS event. */
  final override def apply(event: SQSEvent): RIO[EnvironmentWithLog, Unit] =
    onMessages(event.getRecords.iterator.asScala.toList)

  /**
   * Handles messages from the SQS queue.
   *
   * @param messages The messages to handle.
   * @return An effect that handles the specified messages.
   */
  private def onMessages(messages: List[SQSEvent.SQSMessage]): RIO[EnvironmentWithLog, Unit] = messages match
    case head :: tail =>
      for
        _ <- Option(head.getBody).filterNot(_.isEmpty).fold(RIO.unit) { message =>
          parse[T](message).fold(
            thrown => RIO.service[Log] flatMap (_.error(Messages.failedToParseSqsMessage(message), thrown)),
            onMessage(_)
          )
        }
        _ <- onMessages(tail)
      yield ()
    case Nil =>
      RIO.unit

  /**
   * Handles a message from the SQS queue.
   *
   * @param message The message to handle.
   * @return A task that handles the specified message.
   */
  protected def onMessage(message: T): RIO[EnvironmentWithLog, Unit]

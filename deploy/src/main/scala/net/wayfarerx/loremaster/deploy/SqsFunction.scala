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
package deploy

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

  /* Process a SQS event. */
  final override def apply(event: SQSEvent): RIO[Environment, Unit] = for
    logFactory <- RIO.service[LogFactory]
    log <- logFactory(getClass)
    _ <- onMessages(log, event.getRecords.iterator.asScala)
  yield ()

  /**
   * Handles messages from the SQS queue.
   *
   * @param messages The messages to handle.
   * @return An effect that handles the specified messages.
   */
  private def onMessages(log: Log, messages: Iterator[SQSEvent.SQSMessage]): URIO[Environment, Unit] =
    if !messages.hasNext then URIO.unit else {
      val message = messages.next
      for
        messageOpt <- RIO.fromEither(parseJson[T](message.getBody)).map(Some(_)) catchAll {
          log.error(Messages.failedToDecodeSqsMessage(message.getBody), _) *> URIO.none
        }
        _ <- messageOpt.fold(URIO.unit) { message =>
          onMessage(message) catchAll {
            log.error(Messages.failedToHandleSqsMessage(message.toString), _)
          }
        }
        _ <- onMessages(log, messages)
      yield ()
    }

  /**
   * Handles a message from the SQS queue.
   *
   * @param message The message to handle.
   * @return A task that handles the specified message.
   */
  protected def onMessage(message: T): RIO[Environment, Unit]

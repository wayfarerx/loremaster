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

import scala.jdk.CollectionConverters.given

import com.amazonaws.services.lambda.runtime.events.SQSEvent

import io.circe.Decoder

import zio.{RIO, RLayer, URIO}

import logging.*
import model.*

/**
 * Base type for AWS SQS functions that operate in the specified environment.
 *
 * @tparam E The environment this SQS function operates in.
 * @tparam T The type of message this SQS function handles.
 * @param environment The environment constructor to use.
 */
abstract class SqsFunction[E <: AwsEnv, T: Decoder](
  final override protected val environment: RLayer[AwsEnv, E]
) extends LambdaFunction[E, SQSEvent] :

  /* Process a SQS event. */
  final override def apply(event: SQSEvent): RIO[E, Unit] = for
    log <- RIO.service[LogFactory] flatMap (_ (getClass))
    _ <- log trace Messages.beforeHandlingSqsInput
    _ <- onMessages(log, event.getRecords.iterator.asScala.toList)
    _ <- log trace Messages.afterHandlingSqsInput
  yield ()

  /**
   * Handles messages from the SQS queue.
   *
   * @param messages The messages to handle.
   * @return An effect that handles the specified messages.
   */
  private def onMessages(log: Log, messages: List[SQSEvent.SQSMessage]): URIO[E, Unit] = messages match
    case head :: tail =>
      for
        decoded <- RIO.fromEither(decodeJson[T] apply head.getBody) map (Some(_)) catchAll {
          log.error(Messages failedToDecodeSqsMessage head.getBody, _) map (_ => None)
        }
        _ <- decoded.fold(URIO.unit) { message =>
          onMessage(message) catchAll (log.error(Messages failedToHandleSqsMessage message.toString, _))
        }
        _ <- onMessages(log, tail)
      yield ()
    case Nil => URIO.unit

  /**
   * Handles a message from the SQS queue.
   *
   * @param message The message to handle.
   * @return A task that handles the specified message.
   */
  protected def onMessage(message: T): RIO[E, Unit]
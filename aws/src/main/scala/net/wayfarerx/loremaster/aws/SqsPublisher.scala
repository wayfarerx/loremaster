/* SqsPublisher.scala
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

import scala.concurrent.duration.*
import scala.reflect.ClassTag
import scala.util.control.NonFatal

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import com.amazonaws.services.sqs.model.SendMessageRequest

import io.circe.Encoder

import zio.IO

import event.*

/**
 * Implementation of an AWS SQS event publisher.
 *
 * @tparam T The type of event to publish.
 * @param sqs The SQS client to use.
 */
final class SqsPublisher[T: Encoder : Event : ClassTag](sqs: AmazonSQS) extends Publisher[T] :

  /* Schedule an event for publishing. */
  override def apply(message: T, delay: Option[FiniteDuration]): EventEffect[Unit] =
    val request = SendMessageRequest(Event[T].sqsQueueName, emit(message))
    IO {
      sqs sendMessage delay.filter(_ >= Duration.Zero).fold(request) { backoff =>
        request withDelaySeconds math.min(backoff.toSeconds, SqsPublisher.MaximumDelaySeconds).toInt
      }
    } *> IO.unit catchAll {
      case NonFatal(nonFatal) =>
        IO.fail(EventProblem(
          Messages.failedToSendSqsMessage(request.getMessageBody),
          Option(nonFatal),
          true
        ))
      case fatal =>
        IO.die(fatal)
    }

/**
 * Factory for AWS SQS event publishers.
 */
object SqsPublisher:

  /** The maximum number of seconds that a message can be delayed. */
  val MaximumDelaySeconds: Long = 900L

  /**
   * Creates an AWS SQS event publisher.
   *
   * @tparam T The type of event to publish.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder : Event : ClassTag]: SqsPublisher[T] =
    apply(AmazonSQSClientBuilder.standard)

  /**
   * Creates an AWS SQS event publisher.
   *
   * @tparam T The type of event to publish.
   * @param sqsBuilder The AWS SQS client builder to use.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder : Event : ClassTag](sqsBuilder: AmazonSQSClientBuilder): SqsPublisher[T] =
    apply(sqsBuilder.build)

  /**
   * Creates an AWS SQS event publisher.
   *
   * @tparam T The type of event to publish.
   * @param sqs The AWS SQS client to use.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder : Event : ClassTag](sqs: AmazonSQS): SqsPublisher[T] =
    new SqsPublisher(sqs)
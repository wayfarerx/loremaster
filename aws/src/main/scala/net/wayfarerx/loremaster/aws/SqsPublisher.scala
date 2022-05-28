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

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import com.amazonaws.services.sqs.model.SendMessageRequest

import io.circe.Encoder

import zio.Task

import event.*

/**
 * Implementation of an AWS SQS event publisher.
 *
 * @tparam T The type of event to publish.
 * @param queueUrl  The URL of the SQS queue to publish to.
 * @param sqsClient The SQS client to use.
 */
final class SqsPublisher[T: Encoder](queueUrl: String, sqsClient: AmazonSQS) extends Publisher[T] :

  /* Schedule an event for publishing. */
  override def apply(event: T, delay: Option[FiniteDuration]): Task[Unit] =
    val request = SendMessageRequest(queueUrl, emit(event))
    Task {
      sqsClient sendMessage delay.filter(_ >= Duration.Zero).fold(request) { _delay =>
        request withDelaySeconds math.min(_delay.toSeconds, SqsPublisher.MaximumDelaySeconds).toInt
      }
    } *> Task.unit

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
   * @param queueUrl The URL of the SQS queue to publish to.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder](queueUrl: String): SqsPublisher[T] =
    apply(queueUrl, AmazonSQSClientBuilder.standard)

  /**
   * Creates an AWS SQS event publisher.
   *
   * @tparam T The type of event to publish.
   * @param queueUrl   The URL of the SQS queue to publish to.
   * @param sqsBuilder The AWS SQS client builder to use.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder](queueUrl: String, sqsBuilder: AmazonSQSClientBuilder): SqsPublisher[T] =
    apply(queueUrl, sqsBuilder.build)

  /**
   * Creates an AWS SQS event publisher.
   *
   * @tparam T The type of event to publish.
   * @param queueUrl   The URL of the SQS queue to publish to.
   * @param sqsClient The AWS SQS client to use.
   * @return A new AWS SQS event publisher.
   */
  def apply[T: Encoder](queueUrl: String, sqsClient: AmazonSQS): SqsPublisher[T] =
    new SqsPublisher(queueUrl, sqsClient)
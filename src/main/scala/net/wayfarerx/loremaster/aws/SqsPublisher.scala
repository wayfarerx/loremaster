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

import zio.{RIO, Task}

import configuration.*
import event.*
import model.*

/**
 * Implementation of an AWS SQS event publisher.
 *
 * @tparam T The type of event to publish.
 * @param sqs      The SQS connector to use.
 * @param queueUrl The URL of the SQS queue to publish to.
 */
final class SqsPublisher[T: Encoder](sqs: AmazonSQS, queueUrl: String) extends Publisher[T] :

  /* Schedule an event for publishing. */
  override def apply(event: T, delay: Option[FiniteDuration]): Task[Unit] =
    val request = SendMessageRequest(queueUrl, encodeJson(event))
    Task {
      sqs sendMessage delay.filter(_ >= Duration.Zero).fold(request) { _delay =>
        request withDelaySeconds math.min(_delay.toSeconds, SqsPublisher.MaxDelaySeconds).toInt
      }
    } *> Task.unit

/**
 * Definitions associated with AWS SQS event publishers.
 */
object SqsPublisher:

  /** The maximum allowable delay in seconds. */
  val MaxDelaySeconds: Long = 900L

  /**
   * Creates a configured SQS publisher.
   *
   * @tparam T The type of event to send to the SQS publisher.
   * @param topic The topic of the SQS publisher.
   * @param config The configuration to use.
   * @return A configured SQS publisher.
   */
  def apply[T: Encoder](topic: String, config: Configuration): Task[SqsPublisher[T]] = for
    client <- Task(AmazonSQSClientBuilder.standard.build)
    queueUrl <- config[String](s"$topic.url")
  yield new SqsPublisher(client, queueUrl)
/* SqsPublisherTest.scala
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

import scala.concurrent.duration.*

import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import com.amazonaws.services.sqs.model.{SendMessageRequest, SendMessageResult}

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import zio.Runtime

import org.scalatest.*
import flatspec.*
import matchers.*

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for SQS publishers.
 */
class SqsPublisherTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  import SqsPublisherTest.*

  /** A test queue URL. */
  private val queueUrl = "http://localhost"

  /** A test event. */
  private val testEvent = TestEvent("TEST")

  /** A test delay. */
  private val testDelay = 7

  "SQS publishers" should "publish SQS events" in {
    val sqsClient = mock[AmazonSQS]
    val sendMessageRequest = SendMessageRequest(queueUrl, emitJson(testEvent))
    when(sqsClient.sendMessage(sendMessageRequest)) thenReturn SendMessageResult()
    val publisher = SqsPublisher[TestEvent](queueUrl, sqsClient)
    Runtime.default.unsafeRunTask {
      publisher(testEvent)
    } shouldBe()
    verify(sqsClient).sendMessage(sendMessageRequest)
  }

  it.should("publish delayed SQS events") in {
    val sqsClient = mock[AmazonSQS]
    val sendMessageRequest = SendMessageRequest(queueUrl, emitJson(testEvent)).withDelaySeconds(testDelay)
    when(sqsClient.sendMessage(sendMessageRequest)) thenReturn SendMessageResult()
    val publisher = SqsPublisher[TestEvent](queueUrl, sqsClient)
    Runtime.default.unsafeRunTask {
      publisher(testEvent, testDelay.seconds)
    } shouldBe()
    verify(sqsClient).sendMessage(sendMessageRequest)
  }

  it.should("support default and builder clients") in {
    SqsPublisher[TestEvent](queueUrl) should not be null
    SqsPublisher[TestEvent](queueUrl, AmazonSQSClientBuilder.standard) should not be null
  }

/**
 * Definitions associated with SQS publisher tests.
 */
object SqsPublisherTest:

  /**
   * A test event.
   *
   * @param data The test data.
   */
  case class TestEvent(data: String)

  /**
   * Factory for test events.
   */
  object TestEvent extends (String => TestEvent) :

    /** The given test event encoder. */
    given Encoder[TestEvent] = deriveEncoder[TestEvent]
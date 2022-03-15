/* SqsFunctionTest.scala
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

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import zio.{RIO, RLayer, Task, ZLayer}

import org.scalatest.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.*

import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for SQS function templates.
 */
class SqsFunctionTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  import SqsFunctionTest.*

  "SQS function templates" should "operate in the specified environment" in {
    var messages = 0
    val function = TestFunction { (actual, expected) =>
      Task {
        actual shouldBe expected
        messages += 1
      }
    }
    val context = mock[Context]
    val logger = mock[LambdaLogger]
    when(context.getLogger) thenReturn logger
    val request = SQSEvent()
    val message = SQSEvent.SQSMessage()
    message.setBody(emitJson(TestMessage.Valid))
    request.setRecords((message :: Nil).asJava)
    function.handleRequest(request, context) shouldBe Messages.okay
    messages shouldBe 1
  }

/**
 * Definitions associated with SQS function tests.
 */
object SqsFunctionTest:

  /**
   * A test message.
   *
   * @param data The test data.
   */
  case class TestMessage(data: String)

  /**
   * Factory for test messages.
   */
  object TestMessage extends (String => TestMessage):

    /** The given encoder for test messages. */
    given Encoder[TestMessage] = deriveEncoder[TestMessage]

    /** The given decoder for test messages. */
    given Decoder[TestMessage] = deriveDecoder[TestMessage]

    /** The valid test message. */
    val Valid: TestMessage = TestMessage("TEST")

  /**
   * A test SQS function.
   *
   * @param validate The function to validate messages with.
   */
  final class TestFunction(validate: (TestMessage, TestMessage) => Task[Unit]) extends SqsFunction[TestMessage]:

    override type Environment = AwsEnv

    override def environment: RLayer[AwsEnv, Environment] = ZLayer.identity

    override protected def onMessage(message: TestMessage): RIO[Environment, Unit] =
      validate(message, TestMessage.Valid)
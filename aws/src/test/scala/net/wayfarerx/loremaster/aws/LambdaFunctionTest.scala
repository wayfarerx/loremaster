/* LambdaFunctionTest.scala
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

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}

import zio.{RIO, RLayer, Task, ZLayer}

import org.scalatest.*
import flatspec.*
import matchers.*

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for Lambda function templates.
 */
class LambdaFunctionTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  import LambdaFunctionTest.*

  "Lambda function templates" should "operate in the specified environment" in {
    var requests = 0
    val function = TestFunction { (actual, expected) =>
      Task {
        actual shouldBe expected
        requests += 1
      }
    }
    val context = mock[Context]
    val logger = mock[LambdaLogger]
    when(context.getLogger) thenReturn logger
    function.handleRequest(TestRequest.Valid, context) shouldBe Messages.okay
    requests shouldBe 1
  }

/**
 * Definitions associated with Lambda function tests.
 */
object LambdaFunctionTest:

  /**
   * A test request.
   *
   * @param data The test data.
   */
  case class TestRequest(data: String)

  /**
   * Factory for test requests.
   */
  object TestRequest extends (String => TestRequest):

    /** The valid test request. */
    val Valid: TestRequest = TestRequest("TEST")

  /**
   * A test Lambda function.
   *
   * @param validate The function to validate requests with.
   */
  final class TestFunction(validate: (TestRequest, TestRequest) => Task[Unit])
    extends LambdaFunction[TestRequest]
    with RequestHandler[TestRequest, String] :

      override type Environment = FunctionEnv

      override def environment: RLayer[FunctionEnv, Environment] = ZLayer.identity

      override def apply(request: TestRequest): RIO[Environment, Unit] =
        validate(request, TestRequest.Valid)

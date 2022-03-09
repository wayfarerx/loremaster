/* TwitterProblemTest.scala
 *
 * Copyright (c) 2021 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package twitter

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for Twitter problems.
 */
class TwitterProblemTest extends AnyFlatSpec with should.Matchers :

  /** The test message. */
  private val msg = "MSG"

  "TwitterProblem" should "provide a variety of constructors" in {
    val msgProblem = TwitterProblem(msg)
    msgProblem.message shouldBe msg
    msgProblem.cause shouldBe None
    msgProblem.shouldRetry shouldBe false
    val thrown = RuntimeException(msg)
    val causeProblem = TwitterProblem(msg, Some(thrown))
    causeProblem.message shouldBe msg
    causeProblem.cause shouldBe Some(thrown)
    causeProblem.shouldRetry shouldBe false
    val retryProblem = TwitterProblem(msg, shouldRetry = true)
    retryProblem.message shouldBe msg
    retryProblem.cause shouldBe None
    retryProblem.shouldRetry shouldBe true
  }
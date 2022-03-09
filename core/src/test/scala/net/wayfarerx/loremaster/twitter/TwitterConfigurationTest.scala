/* TwitterConfigurationTest.scala
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
package twitter

import scala.concurrent.duration.*

import zio.{Runtime, UIO}

import configuration.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for Twitter credentials.
 */
class TwitterConfigurationTest extends AnyFlatSpec with should.Matchers :

  /** Test Twitter credentials. */
  private val testConfig = TwitterConfiguration(
    "queueUrl",
    "bearerToken"
  )

  /** Test Twitter credentials. */
  private val fullConfig = TwitterConfiguration(
    testConfig.queueName,
    testConfig.bearerToken,
    Some(10.seconds),
    Some(event.Retries.Default)
  )

  "Twitter credentials" should "read from the configuration" in {
    val _testConfig = Configuration {
      case TwitterConfiguration.QueueName => UIO.some(testConfig.queueName)
      case TwitterConfiguration.BearerToken => UIO.some(testConfig.bearerToken)
      case _ => UIO.none
    }
    Runtime.default.unsafeRunTask(TwitterConfiguration(_testConfig)) shouldBe testConfig
    val _fullConfig = Configuration {
      case TwitterConfiguration.QueueName => UIO.some(fullConfig.queueName)
      case TwitterConfiguration.BearerToken => UIO.some(fullConfig.bearerToken)
      case TwitterConfiguration.ConnectionTimeout => UIO(fullConfig.connectionTimeout map (_.toString))
      case TwitterConfiguration.RetryPolicy => UIO(fullConfig.retryPolicy map (_.toString))
      case _ => UIO.none
    }
    Runtime.default.unsafeRunTask(TwitterConfiguration(_fullConfig)) shouldBe fullConfig
  }
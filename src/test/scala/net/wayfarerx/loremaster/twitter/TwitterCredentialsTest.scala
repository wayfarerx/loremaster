/* TwitterCredentialsTest.scala
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

import twitter4j.conf.ConfigurationBuilder

import zio.{Runtime, UIO}

import configuration.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for Twitter credentials.
 */
class TwitterCredentialsTest extends AnyFlatSpec with should.Matchers :

  /** Test Twitter credentials. */
  private val test = TwitterCredentials(
    "consumerKey",
    "consumerSecret",
    "accessToken",
    "accessTokenSecret"
  )

  "Twitter credentials" should "read from the configuration" in {
    val config = Configuration {
      case s"$TwitterPrefix.consumerKey" => UIO some test.consumerKey
      case s"$TwitterPrefix.consumerSecret" => UIO some test.consumerSecret
      case s"$TwitterPrefix.accessToken" => UIO some test.accessToken
      case s"$TwitterPrefix.accessTokenSecret" => UIO some test.accessTokenSecret
      case _ => UIO.none
    }
    Runtime.default unsafeRunTask TwitterCredentials(config) shouldBe test
  }

  it should "configure twitter4j" in {
    val config = test.configure(ConfigurationBuilder()).build
    config.getOAuthConsumerKey shouldBe test.consumerKey
    config.getOAuthConsumerSecret shouldBe test.consumerSecret
    config.getOAuthAccessToken shouldBe test.accessToken
    config.getOAuthAccessTokenSecret shouldBe test.accessTokenSecret
  }
/* TweetServiceTest.scala
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

import java.io.IOException

import scala.concurrent.duration.*

import twitter4j.{HttpResponseCode, Twitter, TwitterException}

import zio.{Runtime, UIO}

import configuration.*
import event.*
import logging.*
import model.*

import org.scalatest.*
import flatspec.*
import matchers.*

import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar

/**
 * Test case for the tweet service.
 */
class TweetServiceTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  /** A test tweet event. */
  private val testEvent = TweetEvent(Book.of("A", "B"))

  "The tweet service" should "send tweets normally" in {
    val logFactory = mock[LogFactory]
    when(logFactory.log[TweetService]) thenReturn UIO(Log.NoOp)
    val twitter = mock[Twitter]
    val publisher = mock[Publisher[TweetEvent]]
    Runtime.default unsafeRunTask
      TweetService(Configuration.empty, logFactory, twitter, publisher).flatMap(_(testEvent)) shouldBe ()
    verify(twitter).updateStatus(testEvent.book.toString)
    verifyNoInteractions(publisher)
  }

  it should "retry tweets if there are network issues" in {
    val nextEvent = testEvent.copy(retry = Some(1))
    val logFactory = mock[LogFactory]
    when(logFactory.log[TweetService]) thenReturn UIO(Log.NoOp)
    val twitter = mock[Twitter]
    val thrown = TwitterException(IOException("network failed"))
    when(twitter.updateStatus(testEvent.book.toString)) thenThrow thrown
    val publisher = mock[Publisher[TweetEvent]]
    when(publisher.apply(nextEvent, event.Retries.Backoff.Default.delay)) thenReturn UIO.unit
    Runtime.default unsafeRunTask
      TweetService(Configuration.empty, logFactory, twitter, publisher).flatMap(_(testEvent)) shouldBe ()
    verify(twitter).updateStatus(testEvent.book.toString)
    verify(publisher).apply(nextEvent, event.Retries.Backoff.Default.delay)
  }

  it should "retry tweets if there are rate limiting issues" in {
    val nextEvent = testEvent.copy(retry = Some(1))
    val logFactory = mock[LogFactory]
    when(logFactory.log[TweetService]) thenReturn UIO(Log.NoOp)
    val twitter = mock[Twitter]
    val thrown = TwitterException("rate limit exceeded", null, HttpResponseCode.TOO_MANY_REQUESTS)
    when(twitter.updateStatus(testEvent.book.toString)) thenThrow thrown
    val publisher = mock[Publisher[TweetEvent]]
    when(publisher.apply(nextEvent, event.Retries.Backoff.Default.delay)) thenReturn UIO.unit
    Runtime.default unsafeRunTask
      TweetService(Configuration.empty, logFactory, twitter, publisher).flatMap(_(testEvent)) shouldBe ()
    verify(twitter).updateStatus(testEvent.book.toString)
    verify(publisher).apply(nextEvent, event.Retries.Backoff.Default.delay)
  }

  it should "retry tweets if there are other issues" in {
    val nextEvent = testEvent.copy(retry = Some(1))
    val logFactory = mock[LogFactory]
    when(logFactory.log[TweetService]) thenReturn UIO(Log.NoOp)
    val twitter = mock[Twitter]
    val thrown = TwitterException(IllegalStateException("oh no"))
    when(twitter.updateStatus(testEvent.book.toString)) thenThrow thrown
    val publisher = mock[Publisher[TweetEvent]]
    when(publisher.apply(nextEvent, event.Retries.Backoff.Default.delay)) thenReturn UIO.unit
    Runtime.default unsafeRunTask
      TweetService(Configuration.empty, logFactory, twitter, publisher).flatMap(_(testEvent)) shouldBe ()
    verify(twitter).updateStatus(testEvent.book.toString)
    verify(publisher).apply(nextEvent, event.Retries.Backoff.Default.delay)
  }

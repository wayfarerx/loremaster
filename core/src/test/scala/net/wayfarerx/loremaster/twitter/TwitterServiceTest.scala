/* TwitterServiceTest.scala
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

import java.io.IOException

import scala.concurrent.duration.*

import zio.{IO, Runtime, Task}

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
 * Test case for the Twitter service.
 */
class TwitterServiceTest extends AnyFlatSpec with should.Matchers with MockitoSugar :

  /** A test Twitter event. */
  private val testEvent = TwitterEvent(Book.of("A", "B"))

  "TwitterService" should "post tweets normally" in {
    val client = mock[TwitterClient]
    val publisher = mock[Publisher[TwitterEvent]]
    when(client.postTweet(testEvent.book)) thenReturn IO.unit
    Runtime.default.unsafeRunTask {
      TwitterService(Log.NoOp, event.RetryPolicy.Default, client, publisher).apply(testEvent)
    } shouldBe()
    verify(client).postTweet(testEvent.book)
    verifyNoInteractions(publisher)
  }

  it.should("retry flagged problems when posting tweets") in {
    val client = mock[TwitterClient]
    val publisher = mock[Publisher[TwitterEvent]]
    val problem = TwitterProblem("PROBLEM", None, true)
    when(client.postTweet(testEvent.book)) thenReturn IO.fail(problem)
    when(publisher.apply(testEvent.next, RetryPolicy.Backoff.Default(testEvent))) thenReturn Task.unit
    Runtime.default.unsafeRunTask {
      TwitterService(Log.NoOp, RetryPolicy.Default, client, publisher).apply(testEvent)
    } shouldBe()
    verify(client).postTweet(testEvent.book)
    verify(publisher).apply(testEvent.next, RetryPolicy.Backoff.Default(testEvent))
  }

  it.should("propagate fatal problems when posting tweets") in {
    val retryPolicy = RetryPolicy(termination = RetryPolicy.Termination.LimitAttempts(0))
    val client = mock[TwitterClient]
    val publisher = mock[Publisher[TwitterEvent]]
    val problem = TwitterProblem("PROBLEM", Some(RuntimeException()))
    when(client.postTweet(testEvent.book)) thenReturn IO.fail(problem)
    Runtime.default.unsafeRunTask {
      TwitterService(Log.NoOp, retryPolicy, client, publisher)(testEvent) catchSome {
        case _problem if _problem == problem => Task.unit
      }
    } shouldBe()
    verify(client).postTweet(testEvent.book)
    verifyNoInteractions(publisher)
  }
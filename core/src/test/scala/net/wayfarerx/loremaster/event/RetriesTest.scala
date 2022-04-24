/* RetriesTest.scala
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
package event

import java.time.Instant

import scala.concurrent.duration.*

import event.RetryPolicy.{Backoff, Default, Termination}
import configuration.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for retry policies.
 */
class RetriesTest extends AnyFlatSpec with should.Matchers :

  /** Test events can be retried. */
  private[this] given Event[TestEvent] = new Event[TestEvent] :

    override def previousAttempts(event: TestEvent): Int = event.previousAttempts

    override def createdAt(event: TestEvent): Instant = event.createdAt

    override def nextAttempt(event: TestEvent): TestEvent = event.nextAttempt

  /** A delay to use while testing. */
  private[this] val delay = 1.micro

  /** A backoff to use while testing. */
  private[this] val backoff = Backoff.Constant(delay)

  "RetryPolicy" should "support constant backoff" in {
    val event1 = TestEvent()
    val event2 = event1.nextAttempt
    val event3 = event2.nextAttempt
    val retryPolicy = Default.copy(backoff = Backoff.Constant(delay))
    retryPolicy(event1) shouldBe Some(delay)
    retryPolicy(event2) shouldBe Some(delay)
  }

  it.should("support linear backoff") in {
    val event1 = TestEvent()
    val event2 = event1.nextAttempt
    val event3 = event2.nextAttempt
    val retryPolicy = Default.copy(backoff = Backoff.Linear(delay))
    retryPolicy(event1) shouldBe Some(delay)
    retryPolicy(event2) shouldBe Some(delay * 2)
  }

  it.should("support golden backoff") in {
    val event1 = TestEvent()
    val event2 = event1.nextAttempt
    val event3 = event2.nextAttempt
    val retryPolicy = Default.copy(backoff = Backoff.Golden(delay))
    retryPolicy(event1) shouldBe Some(delay)
    retryPolicy(event2) shouldBe Some(delay * 2)
  }

  it.should("support retry limits") in {
    val event1 = TestEvent()
    val event2 = event1.nextAttempt
    val retryPolicy = event.RetryPolicy(backoff, Termination.LimitRetries(1))
    retryPolicy(event1) shouldBe Some(delay)
    retryPolicy(event2) shouldBe None
  }

  it.should("support duration limits") in {
    val event1 = TestEvent()
    val retryPolicy = event.RetryPolicy(backoff, Termination.LimitDuration(0.millis))
    retryPolicy(event1) shouldBe None
  }

  it.should("support parsing configurations") in {
    val config = Configuration.Data[event.RetryPolicy]
    config("") shouldBe None
    config("3 seconds") shouldBe Some(Default.copy(backoff = Backoff.Constant(3.seconds)))
    config("+4 seconds") shouldBe Some(Default.copy(backoff = Backoff.Linear(4.seconds)))
    config("~5 seconds") shouldBe Some(Default.copy(backoff = Backoff.Golden(5.seconds)))
    config(":") shouldBe None
    config(":3") shouldBe Some(Default.copy(termination = Termination.LimitRetries(3)))
    config(":3 seconds") shouldBe Some(Default.copy(termination = Termination.LimitDuration(3.seconds)))
    config("3 seconds:3") shouldBe Some(event.RetryPolicy(Backoff.Constant(3.seconds), Termination.LimitRetries(3)))
    config("+4 seconds:4") shouldBe Some(event.RetryPolicy(Backoff.Linear(4.seconds), Termination.LimitRetries(4)))
    config("~5 seconds:5") shouldBe Some(event.RetryPolicy(Backoff.Golden(5.seconds), Termination.LimitRetries(5)))
  }

  /** An event to test with. */
  private[this] case class TestEvent(createdAt: Instant = Instant.now, previousAttempts: Int = 0):

    /** Create the next attempt at this event. */
    def nextAttempt: TestEvent = copy(previousAttempts = previousAttempts + 1)
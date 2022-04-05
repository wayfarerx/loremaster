/* PublisherTest.scala
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

import scala.concurrent.duration.*

import zio.{Runtime, Task}

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for event publishers.
 */
class PublisherTest extends AnyFlatSpec with should.Matchers :

  "Publisher" should "support alternate invocations" in {
    var _event = ""
    var _delay = Option(1.second)
    val publisher = new Publisher[String]:
      override def apply(event: String, delay: Option[FiniteDuration]): Task[Unit] = Task {
        _event = event
        _delay = delay
      }
    Runtime.default.unsafeRunTask(publisher.apply("test1"))
    _event shouldBe "test1"
    _delay shouldBe None
    Runtime.default.unsafeRunTask(publisher.apply("test2", 5.seconds))
    _event shouldBe "test2"
    _delay shouldBe Some(5.seconds)
  }



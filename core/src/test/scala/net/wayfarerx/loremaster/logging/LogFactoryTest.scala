/* LogFactoryTest.scala
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
package logging

import zio.{Runtime, UIO}

import configuration.*

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the log factory service.
 */
class LogFactoryTest extends AnyFlatSpec with should.Matchers :

  "A log factory" should "create configured log services" in {
    val config = Configuration {
      case "LogFactoryTest.log.level" => UIO.some(Log.Level.Debug.toString)
      case _ => UIO.none
    }
    var emitted = Vector.empty[(Log.Level, String, Option[Throwable])]
    val logs = LogFactory(config, (level, message, thrown) => UIO(emitted :+= (level, message, thrown)))
    val effect = for
      log <- logs.log[LogFactoryTest]
      _ <- log.trace("trace")
      _ <- log.info("info")
      _ <- log.error("error")
    yield ()
    Runtime.default.unsafeRunTask(effect) shouldBe()
    emitted shouldBe Vector(
      (Log.Level.Info, s"${classOf[LogFactoryTest].getSimpleName}: info", None),
      (Log.Level.Error, s"${classOf[LogFactoryTest].getSimpleName}: error", None)
    )
  }
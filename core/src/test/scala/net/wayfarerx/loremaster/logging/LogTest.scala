/* LogTest.scala
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
 * Test case for the log service.
 */
class LogTest extends AnyFlatSpec with should.Matchers :

  "A log" should "log all events" in {
    var emitted = Vector.empty[(Log.Level, String, Option[Throwable])]
    val log = Log("", Log.Level.Trace, (level, message, thrown) => UIO(emitted :+= (level, message, thrown)))
    val thrown = new RuntimeException
    val effect = for
      _ <- log.trace("trace")
      _ <- log.trace("trace", thrown)
      _ <- log.debug("debug")
      _ <- log.debug("debug", thrown)
      _ <- log.info("info")
      _ <- log.info("info", thrown)
      _ <- log.warn("warn")
      _ <- log.warn("warn", thrown)
      _ <- log.error("error")
      _ <- log.error("error", thrown)
    yield ()
    Runtime.default.unsafeRunTask(effect) shouldBe()
    emitted shouldBe Vector(
      (Log.Level.Trace, "trace", None),
      (Log.Level.Trace, "trace", Some(thrown)),
      (Log.Level.Debug, "debug", None),
      (Log.Level.Debug, "debug", Some(thrown)),
      (Log.Level.Info, "info", None),
      (Log.Level.Info, "info", Some(thrown)),
      (Log.Level.Warn, "warn", None),
      (Log.Level.Warn, "warn", Some(thrown)),
      (Log.Level.Error, "error", None),
      (Log.Level.Error, "error", Some(thrown))
    )
  }

  it.should("drop events under the threshold") in {
    var emitted = Vector.empty[(Log.Level, String, Option[Throwable])]
    val log = Log("test", Log.Level.Warn, (level, message, thrown) => UIO(emitted :+= (level, message, thrown)))
    val effect = for
      _ <- log(Log.Level.Trace, "trace")
      _ <- log(Log.Level.Debug, "debug")
      _ <- log(Log.Level.Info, "info")
      _ <- log(Log.Level.Warn, "warn")
      _ <- log(Log.Level.Error, "error")
    yield ()
    Runtime.default.unsafeRunTask(effect) shouldBe()
    emitted shouldBe Vector(
      (Log.Level.Warn, "test: warn", None),
      (Log.Level.Error, "test: error", None)
    )
  }

  "Log.Level" should "support decoding configuration data" in {
    Configuration.Data[Log.Level].apply(Log.Level.Trace.toString) shouldBe Some(Log.Level.Trace)
    Configuration.Data[Log.Level].apply(Log.Level.Debug.toString) shouldBe Some(Log.Level.Debug)
    Configuration.Data[Log.Level].apply(Log.Level.Info.toString) shouldBe Some(Log.Level.Info)
    Configuration.Data[Log.Level].apply(Log.Level.Warn.toString) shouldBe Some(Log.Level.Warn)
    Configuration.Data[Log.Level].apply(Log.Level.Error.toString) shouldBe Some(Log.Level.Error)
    Configuration.Data[Log.Level].apply("") shouldBe None
  }
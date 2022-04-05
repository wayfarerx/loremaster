/* LogEmitterTest.scala
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

import org.scalatest.*
import flatspec.*
import matchers.*

/**
 * Test case for the log emitter.
 */
class LogEmitterTest extends AnyFlatSpec with should.Matchers :

  "A log emitter" should "emit formatted log messages" in {
    var emitted = Vector.empty[String]
    val emitter = LogEmitter.formatted(s => UIO(emitted :+= s))
    val thrownA = new RuntimeException
    val thrownB = new RuntimeException("msg")
    val effect = for
      _ <- emitter(Log.Level.Trace, "trace", None)
      _ <- emitter(Log.Level.Trace, "trace", Some(thrownA))
      _ <- emitter(Log.Level.Debug, "debug", None)
      _ <- emitter(Log.Level.Debug, "debug", Some(thrownB))
      _ <- emitter(Log.Level.Info, "info", None)
      _ <- emitter(Log.Level.Info, "info", Some(thrownA))
      _ <- emitter(Log.Level.Warn, "warn", None)
      _ <- emitter(Log.Level.Warn, "warn", Some(thrownB))
      _ <- emitter(Log.Level.Error, "error", None)
      _ <- emitter(Log.Level.Error, "error", Some(thrownA))
    yield ()
    Runtime.default.unsafeRunTask(effect) shouldBe()
    emitted shouldBe Vector(
      "TRACE trace.",
      "TRACE trace, caused by java.lang.RuntimeException.",
      "DEBUG debug.",
      "DEBUG debug, caused by java.lang.RuntimeException(msg).",
      "INFO  info.",
      "INFO  info, caused by java.lang.RuntimeException.",
      "WARN  warn.",
      "WARN  warn, caused by java.lang.RuntimeException(msg).",
      "ERROR error!",
      "ERROR error, caused by java.lang.RuntimeException!"
    )
  }
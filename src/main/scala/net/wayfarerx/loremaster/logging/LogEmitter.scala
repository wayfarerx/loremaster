/* LogEmitter.scala
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

import zio.UIO

/**
 * Definition of the log emitter API.
 */
trait LogEmitter extends ((Log.Level, String, Option[Throwable]) => UIO[Unit])

/**
 * Factory for log emitters.
 */
object LogEmitter extends (((Log.Level, String, Option[Throwable]) => UIO[Unit]) => LogEmitter) :

  /** The maximum length of log level strings. */
  private[this] val MaxLevelLength = Log.Level.values.map(_.toString.size).max

  /**
   * Creates a new log emitter that emits log entries.
   *
   * @param f The function that implements the log entry emitter.
   * @return A new log emitter that emits log entries.
   */
  override def apply(f: (Log.Level, String, Option[Throwable]) => UIO[Unit]): LogEmitter = f(_, _, _)

  /**
   * Creates a new log emitter that emits formatted strings.
   *
   * @param f The function that implements the formatted string log emitter.
   * @return A new log emitter that emits formatted strings.
   */
  def formatted(f: String => UIO[Unit]): LogEmitter = apply((l, m, t) => f apply format(l, m, t))

  /**
   * Formats a log entry string from the specified elements.
   *
   * @param level   The level being logged at.
   * @param message The message being logged.
   * @param thrown  The exception that was thrown.
   * @return A log entry string from the specified elements.
   */
  def format(level: Log.Level, message: String, thrown: Option[Throwable]): String =
    val levelString = level.toString
    val _level = levelString.toUpperCase + Messages.Space * (MaxLevelLength - levelString.length + 1)
    val _thrown = thrown.fold("")(Messages.causedBy)
    val suffix = if level == Log.Level.Error then Messages.Exclamation else Messages.Period
    _level + message + _thrown + suffix


/* Log.scala
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
package logging

import zio.UIO

/**
 * Definition of the log API.
 */
trait Log:

  import Log.Level

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline final def trace(message: => String, thrown: => Option[Throwable] = None): UIO[Unit] =
    apply(Level.Trace, message, thrown)

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def trace(message: => String, thrown: Throwable): UIO[Unit] =
    apply(Level.Trace, message, thrown)

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline final def debug(message: => String, thrown: => Option[Throwable] = None): UIO[Unit] =
    apply(Level.Debug, message, thrown)

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def debug(message: => String, thrown: Throwable): UIO[Unit] =
    apply(Level.Debug, message, thrown)

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline final def info(message: => String, thrown: => Option[Throwable] = None): UIO[Unit] =
    apply(Level.Info, message, thrown)

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def info(message: => String, thrown: Throwable): UIO[Unit] =
    apply(Level.Info, message, thrown)

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline final def warn(message: => String, thrown: => Option[Throwable] = None): UIO[Unit] =
    apply(Level.Warn, message, thrown)

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def warn(message: => String, thrown: Throwable): UIO[Unit] =
    apply(Level.Warn, message, thrown)

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline final def error(message: => String, thrown: => Option[Throwable] = None): UIO[Unit] =
    apply(Level.Error, message, thrown)

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def error(message: => String, thrown: Throwable): UIO[Unit] =
    apply(Level.Error, message, thrown)

  /**
   * Creates a log entry at the specified level.
   *
   * @param level The level to log at.
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def apply(level: Level, message: => String, thrown: => Option[Throwable] = None): UIO[Unit]

  /**
   * Creates a log entry at the specified level.
   *
   * @param level The level to log at.
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  final def apply(level: Level, message: => String, thrown: Throwable): UIO[Unit] =
    apply(level, message, Option(thrown))

object Log:

  enum Level:
    case Trace, Debug, Info, Warn, Error

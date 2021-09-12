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
package service

import zio.{Task, UIO}

/**
 * Definition of the log service API.
 */
trait Log:

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def trace(message: => String, thrown: Option[Throwable] = None): UIO[Unit]

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def trace(message: => String, thrown: Throwable): UIO[Unit] = trace(message, Option(thrown))

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def debug(message: => String, thrown: Option[Throwable] = None): UIO[Unit]

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def debug(message: => String, thrown: Throwable): UIO[Unit] = debug(message, Option(thrown))

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def info(message: => String, thrown: Option[Throwable] = None): UIO[Unit]

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def info(message: => String, thrown: Throwable): UIO[Unit] = info(message, Option(thrown))

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def warn(message: => String, thrown: Option[Throwable] = None): UIO[Unit]

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def warn(message: => String, thrown: Throwable): UIO[Unit] = warn(message, Option(thrown))

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  def error(message: => String, thrown: Option[Throwable] = None): UIO[Unit]

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline final def error(message: => String, thrown: Throwable): UIO[Unit] = error(message, Option(thrown))

/**
 * Definitions associated with log services.
 */
object Log:
  log =>

  import org.slf4j.Logger
  import zio.{Has, URIO}

  /**
   * Creates a log service that uses the specified logger.
   *
   * @param logger The logger to use.
   * @return A log service that uses the specified logger.
   */
  def apply(logger: Logger): Log = Live(logger)

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline def trace(message: => String, thrown: Option[Throwable] = None): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.trace(message, thrown))

  /**
   * Creates a trace-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline def trace(message: => String, thrown: Throwable): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.trace(message, Option(thrown)))

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline def debug(message: => String, thrown: Option[Throwable] = None): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.debug(message, thrown))

  /**
   * Creates a debug-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline def debug(message: => String, thrown: Throwable): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.debug(message, Option(thrown)))

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline def info(message: => String, thrown: Option[Throwable] = None): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.info(message, thrown))

  /**
   * Creates an info-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline def info(message: => String, thrown: Throwable): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.info(message, Option(thrown)))

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline def warn(message: => String, thrown: Option[Throwable] = None): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.warn(message, thrown))

  /**
   * Creates a warn-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline def warn(message: => String, thrown: Throwable): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.warn(message, Option(thrown)))

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The optional exception to log.
   */
  inline def error(message: => String, thrown: Option[Throwable] = None): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.error(message, thrown))

  /**
   * Creates an error-level log entry.
   *
   * @param message The message to log.
   * @param thrown  The exception to log.
   */
  inline def error(message: => String, thrown: Throwable): URIO[Has[Log], Unit] =
    URIO.service flatMap (_.error(message, Option(thrown)))

  /**
   * The live log implementation.
   *
   * @param logger The logger to use.
   */
  private case class Live(logger: Logger) extends Log :

    /* Create a trace-level log entry. */
    override def trace(message: => String, thrown: Option[Throwable]) =
      if !logger.isTraceEnabled then unit else
        UIO(thrown.fold(logger.trace(message))(logger.trace(message, _)))

    /* Create a debug-level log entry. */
    override def debug(message: => String, thrown: Option[Throwable]) =
      if !logger.isDebugEnabled then unit else
        UIO(thrown.fold(logger.debug(message))(logger.debug(message, _)))

    /* Create an info-level log entry. */
    override def info(message: => String, thrown: Option[Throwable]) =
      if !logger.isInfoEnabled then unit else
        UIO(thrown.fold(logger.info(message))(logger.info(message, _)))

    /* Create a warn-level log entry. */
    override def warn(message: => String, thrown: Option[Throwable]) =
      if !logger.isWarnEnabled then unit else
        UIO(thrown.fold(logger.warn(message))(logger.warn(message, _)))

    /* Create an error-level log entry. */
    override def error(message: => String, thrown: Option[Throwable]) =
      if !logger.isErrorEnabled then unit else
        UIO(thrown.fold(logger.error(message))(logger.error(message, _)))

  /**
   * Definition of the log factory service API.
   */
  trait Factory extends (String => Task[Log])

  /**
   * Definitions associated with log factories.
   */
  object Factory:

    import zio.RIO

    /** The live log factory layer. */
    val live: zio.TaskLayer[Has[Factory]] =
      zio.ZLayer.fromEffect {
        Task(org.slf4j.LoggerFactory.getILoggerFactory) map { factory =>
          name => Task(factory.getLogger(name)) map (Log(_))
        }
      }

    /**
     * Creates a log with the specified name.
     *
     * @param name The name of the log to create.
     * @return A log with the specified name.
     */
    inline def apply(name: String): RIO[Has[Factory], Log] =
      RIO.service flatMap (_ (name))
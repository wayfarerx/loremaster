/* AwsLogging.scala
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
package aws

import com.amazonaws.services.lambda.runtime.LambdaLogger

import zio.{IO, UIO}

import configuration.*
import logging.*

/**
 * An implementation of the logging service that uses an AWS Lambda logger.
 *
 * @param config The configuration to use.
 * @param logger The AWS Lambda logger to use.
 */
final class AwsLogging(config: Configuration, logger: LambdaLogger) extends Logging :

  /* Create a log with the specified name. */
  override def apply(name: String): LoggingEffect[Log] = for
    threshold <- config.getOrElse(s"$name.logging", Log.Level.Warn) catchAll { thrown =>
      IO.fail(LoggingProblem(Messages.invalidLoggingConfiguration(name), Some(thrown)))
    }
  yield AwsLogging.Logger(name, threshold, logger)

/**
 * Factory for AWS logging implementations.
 */
object AwsLogging extends ((Configuration, LambdaLogger) => AwsLogging) :

  /** The maximum length of log level strings. */
  private[this] val MaxLevelLength = Log.Level.values.map(_.toString.size).max

  /**
   * Creates an AWS logging implementation.
   *
   * @param config The configuration to use.
   * @param logger The AWS Lambda logger to use.
   * @return A new AWS logging implementation.
   */
  override def apply(config: Configuration, logger: LambdaLogger): AwsLogging = new AwsLogging(config, logger)

  /**
   * The AWS implementation of the log API.
   *
   * @param name      The name of this log.
   * @param threshold The minimum log level to record.
   * @param logger    The AWS Lambda logger to use.
   */
  private final class Logger(name: String, threshold: Log.Level, logger: LambdaLogger) extends Log :

    /* Create a log entry at the specified level. */
    override def apply(level: Log.Level, message: => String, thrown: Option[Throwable]): UIO[Unit] =
      if Ordering[Log.Level].lt(level, threshold) then UIO.unit else
        val levelString = level.toString.toUpperCase
        val prefix = levelString + " " * (MaxLevelLength - levelString.length)
        UIO(logger.log(s"""$prefix $name $message${thrown.fold("")(cause => s" caused by ${describe(cause)}")}."""))


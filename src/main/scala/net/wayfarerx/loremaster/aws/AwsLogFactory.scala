/* AwsLogFactory.scala
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
package aws

import com.amazonaws.services.lambda.runtime.LambdaLogger

import zio.{Has, RIO, RLayer, Task, UIO, ZLayer}

import configuration.*
import logging.*

/**
 * Implementation of the AWS log factory service.
 *
 * @param threshold The minimum log level to propagate.
 * @param logger    The lambda logger to use.
 */
final class AwsLogFactory(threshold: Log.Level, logger: LambdaLogger) extends LogFactory :

  /* Create a new log instance with the specified name. */
  override def apply(name: String): UIO[Log] = UIO(AwsLog(name))

  /**
   * The AWS lambda log adapter.
   *
   * @param name The name of this log adapter.
   */
  private[this] final class AwsLog(name: String) extends Log :

    /* Create a log entry at the specified level. */
    override def apply(level: Log.Level, message: => String, thrown: => Option[Throwable]) =
      if level.ordinal < threshold.ordinal then UIO.unit else
        val cause = thrown map (t => s"${t.getClass.getSimpleName}(${Option(t.getMessage) getOrElse ""})")
        UIO(logger.log(s"$name: $message${cause.fold("")(c => s", caused by $c")}."))

/**
 * Factory for AWS log factories.
 */
object AwsLogFactory extends ((Log.Level, LambdaLogger) => AwsLogFactory) :

  /** The live AWS log factory layer. */
  val live: RLayer[Has[Configuration] & Has[LambdaLogger], Has[LogFactory]] = ZLayer fromEffect {
    for
      config <- RIO.service[Configuration]
      threshold <- config.get[Log.Level]("logging") map (_ getOrElse Log.Level.Error)
      logger <- RIO.service[LambdaLogger]
    yield apply(threshold, logger)
  }

  /* Create a new AWS log factory. */
  override def apply(threshold: Log.Level, logger: LambdaLogger) = new AwsLogFactory(threshold, logger)
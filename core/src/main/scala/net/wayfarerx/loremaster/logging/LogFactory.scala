/* LogFactory.scala
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

import scala.reflect.ClassTag

import zio.{Task, UIO}

import configuration.*

/**
 * Definition of the log factory API.
 */
trait LogFactory extends (String => Task[Log]) :

  /**
   * Creates a log for the specified class.
   *
   * @param cls The class to create the log for.
   * @return A log for the specified class.
   */
  final def apply(cls: Class[_]): Task[Log] = apply(cls.getSimpleName)

  /**
   * Creates a log for the specified type.
   *
   * @tparam T The type to create the log for.
   * @return A log for the specified type.
   */
  inline final def log[T: ClassTag]: Task[Log] = apply(summon[ClassTag[T]].runtimeClass)

/**
 * Definitions associated with log factories.
 */
object LogFactory extends ((Configuration, LogEmitter) => LogFactory) :

  /** The log level configuration key. */
  private[this] val Key = "log.level"

  /** A regex that matches sequences of separator characters. */
  private[this] val Separators = """\.+""".r

  /**
   * Creates a log factory backed by a configuration and an emitter.
   *
   * @param config  The configuration to use.
   * @param emitter The emitter to use.
   * @return A log factory backed by a configuration and an emitter.
   */
  override def apply(config: Configuration, emitter: LogEmitter): LogFactory = (name: String) =>
    val path = Separators.split(name).iterator.filterNot(_.isEmpty).toVector
    configuredThreshold(config, path).map(Log(path mkString ".", _, emitter))

  /**
   * Returns the configured logging threshold of the specified path.
   *
   * @param config The configuration to use.
   * @param path The path to return the logging threshold of.
   * @return The configured logging threshold of the specified path.
   */
  private[this] def configuredThreshold(config: Configuration, path: Vector[String]): Task[Log.Level] =
    if path.isEmpty then config.get[Log.Level](Key).map(_ getOrElse Log.Level.Warn) else
      for
        threshold <- config.get[Log.Level](path.mkString("", ".", s".$Key"))
        result <- threshold.fold(configuredThreshold(config, path.init))(UIO(_))
      yield result
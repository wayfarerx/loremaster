/* Logging.scala
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

import scala.reflect.ClassTag

import zio.UIO

/**
 * Definition of the logging service.
 */
trait Logging extends (String => LoggingEffect[Log]) :

  /**
   * Creates a log for the specified class.
   *
   * @param cls The class to create the log for.
   * @return A log for the specified class.
   */
  final def apply(cls: Class[_]): LoggingEffect[Log] = apply(cls.getSimpleName)

  /**
   * Creates a log for the specified type.
   *
   * @tparam T The type to create the log for.
   * @return A log for the specified type.
   */
  final def log[T: ClassTag]: LoggingEffect[Log] = apply(summon[ClassTag[T]].runtimeClass)

/**
 * Definitions associated with the logging service.
 */
object Logging:

  /** A logging service that returns logs that always do nothing. */
  val NoOp: Logging = new Logging:
    override def apply(name: String): LoggingEffect[Log] = UIO(Log.NoOp)
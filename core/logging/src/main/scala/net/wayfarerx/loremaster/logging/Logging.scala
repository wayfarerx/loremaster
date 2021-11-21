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

import zio.Task

/**
 * Definition of the logging API.
 */
trait Logging extends (String => Task[Log]) :

  /**
   * Creates a log for the specified class.
   *
   * @param cls The class to create the log for.
   * @return A log for the specified class.
   */
  final def apply(cls: Class[_]): Task[Log] = apply(cls.getCanonicalName)

  /**
   * Creates a log for the specified type.
   *
   * @tparam T The type to create the log for.
   * @return A log for the specified type.
   */
  inline final def log[T: ClassTag]: Task[Log] = apply(summon[ClassTag[T]].runtimeClass)
/* Parameter.scala
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
package deployments

import scala.concurrent.duration.*

import event.*

/**
 * A type class for supported parameter types.
 *
 * @tparam T The type of parameter that is supported.
 */
trait Parameter[T] extends (T => String) :

  /** The AWS type of the parameter. */
  def _type: String

/**
 * Factory for supported parameter types.
 */
object Parameter:

  /** The given integer parameter type support. */
  given Parameter[Int] = Implementation("Number", _.toString)

  /** The given long parameter type support. */
  given Parameter[Long] = Implementation("Number", _.toString)

  /** The given string parameter type support. */
  given Parameter[String] = Implementation("String", identity)

  /** The given finite duration parameter type support. */
  given Parameter[FiniteDuration] = Implementation("String", _.toString)

  /** The given retry policy parameter type support. */
  given Parameter[RetryPolicy] = Implementation("String", _.toString)

  /**
   * Summons the support for the specified parameter type.
   *
   * @tparam T The type of parameter to summon support for.
   * @return The support for the specified parameter type.
   */
  def apply[T: Parameter]: Parameter[T] = summon[Parameter[T]]

  /**
   * The implementation of the parameter type class.
   *
   * @tparam T The type of parameter to support.
   * @param _type  The name of the supported type.
   * @param encode The function that encodes supported values.
   */
  private final class Implementation[T](override val _type: String, encode: T => String) extends Parameter[T] :

    /* Encode the specified value. */
    override def apply(value: T): String = encode(value)

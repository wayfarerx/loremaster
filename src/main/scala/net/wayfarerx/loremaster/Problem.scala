/* Problem.scala
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

/**
 * A problem with a message and optional cause.
 *
 * @param message The message that describes this problem.
 * @param cause   The optional cause of this problem.
 */
case class Problem(message: String, cause: Option[Throwable]) extends RuntimeException(message) :

  /* Determine the cause of this problem. */
  override def getCause: Throwable = cause getOrElse super.getCause

/**
 * Factory for problems.
 */
object Problem extends ((String, Option[Throwable]) => Problem) :

  /**
   * Creates a problem with the specified message.
   *
   * @param message The message that describes the problem.
   * @return A problem with the specified message.
   */
  inline def apply(message: String): Problem = Problem(message, None)

  /**
   * Creates a problem from the specified cause.
   *
   * @param cause The cause of the problem.
   * @return A problem from the specified cause.
   */
  inline def apply(cause: Throwable): Problem =
    Problem(Option(cause.getMessage) getOrElse cause.getClass.getSimpleName, Some(cause))

  /**
   * Creates a problem with the specified message and cause.
   *
   * @param message The message that describes the problem.
   * @param cause The cause of the problem.
   * @return A problem with the specified message and cause.
   */
  inline def apply(message: String, cause: Throwable): Problem = Problem(message, Some(cause))
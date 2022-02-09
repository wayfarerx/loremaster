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
case class Problem private(message: String, cause: Option[Throwable]) extends RuntimeException(message) :

  /* Determine the cause of this problem. */
  override def getCause: Throwable = cause getOrElse super.getCause

/**
 * The factory for problems.
 */
object Problem:

  /**
   * Creates a problem.
   *
   * @return A new problem.
   */
  def apply(): http.HttpProblem =
    Problem("Problem encountered.", None)


  /**
   * Creates a problem.
   *
   * @param message The message that describes the problem.
   * @return A new problem.
   */
  def apply(message: String): http.HttpProblem =
    resolve(message).fold(apply())(Problem(_, None))

  /**
   * Creates a problem.
   *
   * @param cause The throwable that caused the problem.
   * @return A new problem.
   */
  def apply(cause: Throwable): http.HttpProblem =
    Option(cause).fold(apply())(c => Problem(resolveMessage(c.getMessage), Some(c)))

  /**
   * Creates a problem.
   *
   * @param message The message that describes the problem.
   * @param cause   The throwable that caused the problem.
   * @return A new problem.
   */
  def apply(message: String, cause: Throwable): http.HttpProblem =
    val _cause = Option(cause)
    Problem(resolveMessage(message, _cause.fold("")(_.getMessage)), _cause)

  /**
   * Returns a non-null, non-empty & non-whitespace message that describes a problem.
   *
   * @param message     The message to consult first and return if it is valid.
   * @param findMessage The operation to consult second and return the result of if it is valid.
   * @return A non-null, non-empty & non-whitespace message that describes a problem.
   */
  private def resolveMessage(message: String = "", findMessage: => String = ""): String =
    resolve(message) orElse resolve(findMessage) getOrElse "Problem encountered."

  /**
   * Returns a non-null, non-empty & non-whitespace message.
   *
   * @param message The message to resolve.
   * @return A non-null, non-empty & non-whitespace message.
   */
  private inline def resolve(message: String): Option[String] =
    Option(message) map (_.trim) filterNot (_.isEmpty)
/* HttpProblem.scala
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
package http

import scala.util.control.NoStackTrace

/**
 * A problem encountered when executing an HTTP operation.
 *
 * @param _message    The message that describes this HTTP problem.
 * @param thrown      The optional throwable that caused this HTTP problem, defaults to none.
 * @param shouldRetry True if the HTTP operation should be retried, defaults to false.
 */
class HttpProblem(
  _message: String,
  val thrown: Option[Throwable] = None,
  val shouldRetry: Boolean = false
) extends RuntimeException(_message) with NoStackTrace :

  /** The message that describes this HTTP problem. */
  def message: String = getMessage

  /* Return the cause of this HTTP problem. */
  override def getCause: Throwable = thrown getOrElse super.getCause
/* Problem.scala
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

import scala.util.control.NoStackTrace

/**
 * Base type for problems raised by the application.
 *
 * @param message     The message that describes this problem.
 * @param causedBy    The throwable that caused this problem, defaults to none.
 * @param shouldRetry True if the operation should be retried, defaults to false.
 */
abstract class Problem(
  val message: String,
  val causedBy: Option[Throwable] = None,
  val shouldRetry: Boolean = false
) extends RuntimeException(message) with NoStackTrace :

  /* Return the cause of this Twitter problem. */
  final override def getCause: Throwable = causedBy getOrElse super.getCause
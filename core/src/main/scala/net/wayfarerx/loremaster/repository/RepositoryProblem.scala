/* RepositoryProblem.scala
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
package repository

/**
 * A problem raised by the repository subsystem.
 *
 * @param message     The message that describes this problem.
 * @param causedBy    The throwable that caused this problem, defaults to none.
 * @param shouldRetry True if the operation should be retried, defaults to false.
 */
final class RepositoryProblem(message: String, causedBy: Option[Throwable] = None, shouldRetry: Boolean = false)
  extends Problem(message, causedBy, shouldRetry)
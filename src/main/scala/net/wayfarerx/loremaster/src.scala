/* src.scala
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

import zio.Task

/**
 * Creates a problematic effect from the specified message.
 *
 * @param message The message that describes the problem.
 * @return A problematic effect from the specified message.
 */
inline def fail(message: String): Task[Nothing] = Task.fail(Problem(message))

/**
 * Creates a problematic effect from the specified cause.
 *
 * @param cause The cause of the problem.
 * @return A problematic effect from the specified cause.
 */
inline def fail(cause: Throwable): Task[Nothing] = Task.fail(Problem(cause))

/**
 * Creates a problematic effect with the specified message and cause.
 *
 * @param message The message that describes the problem.
 * @param cause   The cause of the problem.
 * @return A problematic effect with the specified message and cause.
 */
inline def fail(message: String, cause: Throwable): Task[Nothing] = Task.fail(Problem(message, cause))

/**
 * Creates a problematic effect with the specified message and optional cause.
 *
 * @param message The message that describes the problem.
 * @param cause   The optional cause of the problem.
 * @return A problematic effect with the specified message and optional cause.
 */
inline def fail(message: String, cause: Option[Throwable]): Task[Nothing] = Task.fail(Problem(message, cause))
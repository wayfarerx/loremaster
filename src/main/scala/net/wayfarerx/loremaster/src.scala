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

import zio.{Task, UIO}

/** The unit effect. */
val unit: UIO[Unit] = UIO.unit

/** The none effect. */
val none: UIO[Option[Nothing]] = UIO.none

/** The nil effect. */
val nil: UIO[List[Nothing]] = UIO(Nil)

/**
 * Creates a pure effect from the specified value.
 *
 * @tparam T The type of the pure value.
 * @param value The value to use.
 * @return A pure effect from the specified value.
 */
def pure[T](value: => T): UIO[T] = UIO(value)

/**
 * Creates a some effect from the specified value.
 *
 * @tparam T The type of the value.
 * @param value The value to use.
 * @return A some effect from the specified value.
 */
def some[T](value: => T): UIO[Option[T]] = UIO some value

/**
 * Creates a problematic effect from the specified message.
 *
 * @param message The message that describes the problem.
 * @return A problematic effect from the specified message.
 */
def fail(message: String): Task[Nothing] = Task fail Problem(message)

/**
 * Creates a problematic effect with the specified message and cause.
 *
 * @param message The message that describes the problem.
 * @param cause   The cause of the problem.
 * @return A problematic effect with the specified message and cause.
 */
def fail(message: String, cause: Throwable): Task[Nothing] = Task fail Problem(message, cause)
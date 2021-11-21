/* Target.scala
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
package model

/**
 * Base type for transition targets.
 */
sealed trait Target

/**
 * Definition of the transition targets.
 */
object Target:

  /** The ordering of targets. */
  given Ordering[Target] = (x, y) => x match
    case Continue(xToken) => y match
      case Continue(yToken) => Ordering[Token].compare(xToken, yToken)
      case End => -1
    case End => y match
      case Continue(_) => 1
      case End => 0

  /**
   * A transition target that continues to the specified token.
   *
   * @param token The token to continue to.
   */
  case class Continue(token: Token) extends Target

  /**
   * The transition target that ends a sentence.
   */
  case object End extends Target

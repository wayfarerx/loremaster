/* Key.scala
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

import zio.Task

/**
 * Base type for transition keys.
 */
sealed trait Key

/**
 * Definition of the transition keys.
 */
object Key:

  /** The ordering of targets. */
  given Ordering[Key] = (x, y) => x match
    case Start => y match
      case From(_) => -1
      case Start => 0
    case From(xToken) => y match
      case From(yToken) => Ordering[Token].compare(xToken, yToken)
      case Start => 1

  /**
   * The transition key that starts a sentence.
   */
  case object Start extends Key

  /**
   * A transition key that continues from specified token.
   *
   * @param token The token to continue from.
   */
  case class From(token: Token) extends Key


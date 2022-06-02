/* Node.scala
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

import model.*

/**
 * Base type for nodes in a chain.
 */
sealed trait Node

/**
 * Definition of the supported chain nodes.
 */
object Node:

  /**
   * Base type for chain nodes that can be transitioned from.
   */
  sealed trait Source extends Node

  /**
   * Base type for chain nodes that can be transitioned to.
   */
  sealed trait Destination extends Node :

    /** The token this destination points to. */
    def token: Token

  /**
   * The node that starts a chain.
   */
  case object Start extends Source

  /**
   * A node that continues a chain.
   *
   * @param token The token this destination points to.
   */
  case class Continue(token: Token) extends Source with Destination

  /**
   * A node that ends a chain.
   *
   * @param token The token this destination points to.
   */
  case class End(token: Token) extends Destination
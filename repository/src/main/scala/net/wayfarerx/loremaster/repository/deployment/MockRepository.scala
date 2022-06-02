/* MockRepository.scala
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
package deployment

import zio.{IO, UIO}

import model.*

/**
 * A placeholder for a real repository implementation.
 */
object MockRepository extends Repository :

  private val The = Node.Continue(Token.Text("The", "*"))

  private val Lazy = Node.Continue(Token.Text("lazy", "*"))

  private val Shaggy = Node.Continue(Token.Text("shaggy", "*"))

  private val Dog = Node.Continue(Token.Text("dog", "*"))

  private val Barks = Node.Continue(Token.Text("barks", "*"))

  private val Runs = Node.Continue(Token.Text("runs", "*"))

  private val END = Node.End(Token.Text(".", "."))

  /* Return the links to destinations from the specified source node. */
  override def linksFrom(source: Node.Source): RepositoryEffect[List[Link]] = source match
    case Node.Start => UIO(List(Link(The, 1)))
    case The => UIO(List(Link(Lazy, 1), Link(Shaggy, 1)))
    case Lazy | Shaggy => UIO(List(Link(Dog, 1)))
    case Dog => UIO(List(Link(Barks, 1), Link(Runs, 1)))
    case Runs | Barks => UIO(List(Link(END, 1)))
    case unknown => IO.fail(RepositoryProblem(s"Invalid node: $unknown."))
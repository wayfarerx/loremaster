/* Renderer.scala
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
package analysis

import model.*

/**
 * Definition of the analysis renderer.
 */
trait Renderer:

  /**
   * Constructs lore from the specified book.
   *
   * @param book The book to construct lore from.
   * @return Lore constructed from the specified book.
   */
  def analyze(book: Book): AnalysisEffect[Lore]

  /**
   * Constructs a book from the specified lore.
   *
   * @param lore The lore to construct a book from.
   * @return A book constructed from the specified lore.
   */
  def render(lore: Lore): AnalysisEffect[Book]
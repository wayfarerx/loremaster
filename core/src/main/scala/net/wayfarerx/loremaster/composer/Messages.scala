/* Messages.scala
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
package composer

import scala.concurrent.duration.*

import model.*
import repository.*

/**
 * The messages provided by the composer package.
 */
private object Messages:

  def failedToRender(lore: Lore): String =
    s"Failed to render: $lore."

  def emptyParagraph(sentenceCount: Int) =
    s"Cannot create a paragraph with less than one sentence: $sentenceCount."

  def failedToSelectLinks(source: Node.Source) =
    s"Failed to select links from $source."

  def couldNotFindAnyLinks(source: Node.Source) =
    s"Could not find any links from $source."

  def invalidTransition(source: Node.Source, target: Int) =
    s"Invalid transition from $source to $target."

  def composed(book: Book): String =
    s"Composed: $book."

  def failedToTweet(book: Book): String =
    s"Failed to tweet: $book."

  def retryingComposition(event: ComposerEvent, backoff: FiniteDuration) =
    s"Retrying composition after $backoff: $event."

  def failedToRetryComposition(event: ComposerEvent) =
    s"Failed to retry composition: $event."
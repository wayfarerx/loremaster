/* ComposerService.scala
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

import scala.annotation.tailrec

import cats.data.NonEmptyList

import zio.{IO, UIO}
import zio.random.Random

import event.*
import logging.*
import model.*
import nlp.*
import repository.*
import twitter.*

/**
 * The definition of the composer service.
 *
 * @param log         The log to use.
 * @param retryPolicy The retry policy to use.
 * @param repository  The repository to use.
 * @param renderer    The renderer to use.
 * @param twitter     The Twitter event publisher.
 * @param fallback    The composer event publisher to retry with.
 */
final class ComposerService(
  log: Log,
  retryPolicy: RetryPolicy,
  repository: Repository,
  rng: Random.Service,
  renderer: NlpRenderer,
  twitter: Publisher[TwitterEvent],
  fallback: Publisher[ComposerEvent]
) extends (ComposerEvent => ComposerEffect[Unit]) :

  /* Handle a composer event. */
  override def apply(event: ComposerEvent): ComposerEffect[Unit] = {
    for
      lore <- composeLore(event.composition.sentenceCountPerParagraph)
      book <- renderer(lore) catchAll { problem =>
        IO.fail(ComposerProblem(Messages.failedToRender(lore), Option(problem), problem.shouldRetry))
      }
      _ <- log.info(Messages.composed(book))
      _ <- twitter(TwitterEvent(book)) catchAll { problem =>
        IO.fail(ComposerProblem(Messages.failedToTweet(book), Option(problem), problem.shouldRetry))
      }
    yield ()
  } catchSome {
    case problem if problem.shouldRetry =>
      retryPolicy(event).fold(IO.fail(problem)) { backoff =>
        log.warn(Messages.retryingComposition(event, backoff)) *>
          fallback(Event[ComposerEvent].nextAttempt(event), backoff) catchAll { thrown =>
          IO.fail(ComposerProblem(Messages.failedToRetryComposition(event), Option(thrown)))
        }
      }
  }

  /**
   * Compose lore according to the specified composition.
   *
   * @param sentenceCountPerParagraph The number of sentences to compose for each paragraph.
   */
  private def composeLore(sentenceCountPerParagraph: NonEmptyList[Int]): ComposerEffect[Lore] = for
    head <- composeParagraph(sentenceCountPerParagraph.head)
    tail <- NonEmptyList.fromList(sentenceCountPerParagraph.tail).fold(UIO.none)(composeLore(_) map (Option(_)))
  yield tail.fold(Lore.of(head))(lore => lore.copy(paragraphs = head :: lore.paragraphs))

  /**
   * Composes a paragraph with the specified number of sentences.
   *
   * @param sentenceCount The number of sentences to compose.
   * @return A new paragraph with the specified number of sentences.
   */
  private def composeParagraph(sentenceCount: Int): ComposerEffect[Paragraph] =
    if sentenceCount <= 0 then IO.fail(ComposerProblem(Messages.emptyParagraph(sentenceCount))) else for
      head <- composeSentence
      tail <- if sentenceCount == 1 then UIO.none else composeParagraph(sentenceCount - 1) map (Option(_))
    yield tail.fold(Paragraph.of(head))(paragraph => paragraph.copy(sentences = head :: paragraph.sentences))

  /**
   * Composes a sentence.
   *
   * @return A new sentence.
   */
  private def composeSentence: ComposerEffect[Sentence] =

    /* Search the specified list of links for the destination at the specified offset. */
    @tailrec
    def search(links: List[Link], offset: Int): Option[Node.Destination] = links match
      case head :: tail =>
        val next = offset - head.count
        if next < 0 then Option(head.destination) else search(tail, next)
      case Nil =>
        None

    /* Continue building a list of tokens from the specified source. */
    def continue(source: Node.Source): ComposerEffect[NonEmptyList[Token]] = for
      links <- repository.linksFrom(source) catchAll { thrown =>
        IO.fail(ComposerProblem(Messages.failedToSelectLinks(source), Option(thrown), thrown.shouldRetry))
      }
      offset <- links.foldLeft(0)(_ + _.count) match
        case count if count >= 1 => rng.nextIntBounded(count)
        case _ => IO.fail(ComposerProblem(Messages.couldNotFindAnyLinks(source)))
      destination <- search(links, offset)
        .fold(IO.fail(ComposerProblem(Messages.invalidTransition(source, offset))))(UIO(_))
      result <- destination match
        case Node.End(token) => UIO(NonEmptyList.one(token))
        case next@Node.Continue(token) => continue(next) map (token :: _)
    yield result

    continue(Node.Start) map Sentence
/* OpenNLPRenderer.scala
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
package nlp
package deployment

import java.io.{FileNotFoundException, InputStream}
import java.net.URI

import scala.util.control.NonFatal

import cats.data.NonEmptyList

import opennlp.tools.tokenize.{Detokenizer, DetokenizationDictionary, DictionaryDetokenizer}

import zio.{IO, Task, TaskManaged, UIO, ZManaged}

import model.*

/**
 * An NLP renderer powered by OpenNLP.
 *
 * @param detokenizer The NLP detokenizer to use.
 */
final class OpenNLPRenderer(detokenizer: Detokenizer) extends Renderer :

  /* Construct a book from the specified lore. */
  override def render(lore: Lore): NlpEffect[Book] =
    renderParagraphs(lore.paragraphs) map Book

  /* Render a non-empty list of paragraphs. */
  private def renderParagraphs(paragraphs: NonEmptyList[Paragraph]): NlpEffect[NonEmptyList[String]] = for
    head <- renderSentences(paragraphs.head.sentences)
    remaining <- NonEmptyList.fromList(paragraphs.tail).fold(UIO.none)(UIO.some)
    result <- remaining.fold(UIO(NonEmptyList.one(head)))(renderParagraphs(_) map (head :: _))
  yield result

  /* Render a non-empty list of sentences. */
  private def renderSentences(sentences: NonEmptyList[Sentence]): NlpEffect[String] = for
    head <- renderSentence(sentences.head)
    remaining <- NonEmptyList.fromList(sentences.tail).fold(UIO.none)(UIO.some)
    result <- remaining.fold(UIO(head))(renderSentences(_) map (tail => s"$head $tail"))
  yield result

  /* Render a single paragraph. */
  private def renderSentence(sentence: Sentence): NlpEffect[String] = IO {
    detokenizer.detokenize(sentence.tokens.iterator.map {
      case Token.Text(text, _) => text
      case Token.Name(name, _) => name
    }.toArray, Space)
  } catchAll {
    case NonFatal(thrown) => IO.fail(NlpProblem(Messages.failedToRenderSentence(sentence), Some(thrown)))
    case fatal => IO.die(fatal)
  }

/**
 * Factory for NLP renderers powered by OpenNLP.
 */
object OpenNLPRenderer extends (Detokenizer => OpenNLPRenderer) :

  /**
   * Returns a new OpenNLP renderer.
   *
   * @param detokenizer The OpenNLP detokenizer to use.
   * @return A new OpenNLP renderer.
   */
  override def apply(detokenizer: Detokenizer): OpenNLPRenderer = new OpenNLPRenderer(detokenizer)


  /**
   * Creates a new OpenNLP renderer.
   *
   * @param detokenizerDictionary The optional URI of the custom OpenNLP detokenizer dictionary to use.
   * @return A new OpenNLP renderer.
   */
  def create(detokenizerDictionary: Option[URI] = None): Task[OpenNLPRenderer] = for
    _detokenizerDictionary <- load(detokenizerDictionary, DefaultDetokenizerDictionary)
      .use(Task apply DetokenizationDictionary(_))
  yield apply(DictionaryDetokenizer(_detokenizerDictionary))
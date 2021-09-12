/* Synchronize.scala
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
package service

import zio.Task
import model.*

/**
 * Definition of the analysis service API.
 */
trait Analysis:

  /**
   * Constructs lore from a book.
   *
   * @param book The book to construct lore from.
   * @return Lore constructed from the specified book.
   */
  def analyze(book: Book): Task[Option[Lore]]

/**
 * Definitions associated with analysis services.
 */
object Analysis:

  import java.io.InputStream
  import java.net.URL
  import cats.data.NonEmptyList
  import opennlp.tools.namefind.{NameFinderME, TokenNameFinder, TokenNameFinderModel}
  import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
  import opennlp.tools.sentdetect.{SentenceModel, SentenceDetector, SentenceDetectorME}
  import opennlp.tools.tokenize.{TokenizerModel, Tokenizer, TokenizerME}
  import opennlp.tools.util.Span
  import zio.{Has, RIO, TaskManaged, UIO}

  /** The live analysis layer. */
  val live: zio.RLayer[Has[Configuration] & Has[Log.Factory], Has[Analysis]] =
    zio.ZLayer.fromEffect {
      for
        config <- RIO.service[Configuration]
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Analysis].getSimpleName)
        analysis <- apply(
          config.sentenceModel,
          config.tokenizerModel,
          config.partOfSpeechModel,
          config.nameFinderModel,
          log
        )
      yield analysis
    }

  /**
   * Creates an analysis service.
   *
   * @param sentenceModel  The optional path or URI to download the NLP sentence model from.
   * @param tokenizerModel The optional path or URI to download the NLP tokenizer model from.
   * @param posModel       The optional path or URI to download the NLP part of speech model from.
   * @param nameModel      The optional path or URI to download the NLP name finder model from.
   * @param log            The log to append to.
   * @return An analysis service.
   */
  def apply(
    sentenceModel: Option[String],
    tokenizerModel: Option[String],
    posModel: Option[String],
    nameModel: Option[String],
    log: Log
  ): Task[Analysis] = for
    _sentenceModel <- modelData(sentenceModel, "nlp/en-sent.bin") use (Task apply SentenceModel(_))
    _tokenizerModel <- modelData(tokenizerModel, "nlp/en-token.bin") use (Task apply TokenizerModel(_))
    _posModel <- modelData(posModel, default = "nlp/en-pos-maxent.bin") use (Task apply POSModel(_))
    _nameFinderModel <- modelData(nameModel, "nlp/en-ner-person.bin") use (Task apply TokenNameFinderModel(_))
  yield Live(
    SentenceDetectorME(_sentenceModel),
    TokenizerME(_tokenizerModel),
    POSTaggerME(_posModel),
    NameFinderME(_nameFinderModel),
    log
  )

  /**
   * Loads an NLP model from the specified path, URI or the default classpath resouce.
   *
   * @param pathOrUri The optional path or URI to load from.
   * @param default   The location of the default classpath resource.
   * @return An NLP model from the specified path, URI or the default classpath resouce.
   */
  private def modelData(pathOrUri: Option[String], default: String): TaskManaged[InputStream] =
    zio.ZManaged.fromAutoCloseable {
      pathOrUri match
        case Some(_pathOrUri) =>
          for
            url <- Task(URL(if _pathOrUri contains ':' then _pathOrUri else "file:" + _pathOrUri))
            stream <- Task(url.openStream)
          yield stream
        case None =>
          Task(getClass.getClassLoader.getResourceAsStream(default))
    }

  /**
   * Constructs lore from a book.
   *
   * @param book The book to construct lore from.
   * @return Lore constructed from the specified book.
   */
  inline def analyze(book: Book): RIO[Has[Analysis], Option[Lore]] =
    RIO.service flatMap (_.analyze(book))

  /**
   * The live analysis implementation.
   *
   * @param sentenceDetector The NLP sentence detector to use.
   * @param tokenizer        The NLP tokenizer to use.
   * @param posTagger        The part of speech tagger to use.
   * @param nameFinder       The name finder to use.
   * @param log              The log to append to.
   */
  private case class Live(
    sentenceDetector: SentenceDetector,
    tokenizer: Tokenizer,
    posTagger: POSTagger,
    nameFinder: TokenNameFinder,
    log: Log
  ) extends Analysis :

    /* Construct lore from a book. */
    override def analyze(book: Book): Task[Option[Lore]] =
      analyzeParagraphs(book.content.toList) map (NonEmptyList fromList _ map (Lore(book.title, book.author, _)))

    /**
     * Analyzes a list of paragraphs.
     *
     * @param paragraphs The paragraphs to analyze.
     * @return The analyzed list of paragraphs.
     */
    private def analyzeParagraphs(paragraphs: List[String]): Task[List[Paragraph]] = paragraphs match
      case head :: tail =>
        for
          _head <- analyzeParagraph(head)
          _tail <- analyzeParagraphs(tail)
        yield _head.fold(_tail)(_ :: _tail)
      case Nil => nil

    /**
     * Analyzes a single paragraph.
     *
     * @param paragraph The paragraph to analyze.
     * @return The analyzed paragraph.
     */
    private def analyzeParagraph(paragraph: String): Task[Option[Paragraph]] =
      Option(paragraph) filterNot (_.isEmpty) match
        case Some(_paragraph) =>
          for
            sentences <- Task(sentenceDetector.sentDetect(_paragraph)) map (Option(_) filterNot (_.isEmpty))
            result <- sentences.fold(none)(analyzeSentences(_) flatMap some)
          yield result flatMap NonEmptyList.fromList
        case None => none

    /**
     * Analyzes an array of sentences starting at the specified offset.
     *
     * @param sentences The sentences to analyze.
     * @param offset The offset into the array to start at.
     * @return The analyzed list of sentences.
     */
    private def analyzeSentences(sentences: Array[String], offset: Int = 0): Task[List[Sentence]] =
      if offset < 0 || offset >= sentences.length then nil else
        for
          head <- analyzeSentence(sentences(offset))
          tail <- analyzeSentences(sentences, offset + 1)
        yield head.fold(tail)(_ :: tail)

    /**
     * Analyzes a single sentence.
     *
     * @param sentence The sentence to analyze.
     * @return The analyzed sentence.
     */
    private def analyzeSentence(sentence: String): Task[Option[Sentence]] =
      Option(sentence) filterNot (_.isEmpty) match
        case Some(_sentence) =>
          for
            text <- Task(tokenizer.tokenize(_sentence)) map (Option(_) filterNot (_.isEmpty))
            result <- text match
              case Some(_text) =>
                for
                  partsOfSpeech <- Task(posTagger.tag(_text))
                  foundNames <- Task(nameFinder.find(_text))
                  tokens <-
                    val textTokens = _text.iterator.zip(partsOfSpeech.iterator) flatMap { case (txt, pos) =>
                      Option(txt).filterNot(_.isEmpty).map(_txt => Token.Text(_txt, Option(pos) filterNot (_.isEmpty)))
                    }
                    extractNames(foundNames.toList, textTokens.toList)
                yield NonEmptyList fromList tokens
              case None => none
          yield result
        case None => none

    /**
     * Extracts the names in the specified sentence using the supplied spans.
     *
     * @param spans The spans that identify the names to extract.
     * @param sentence The sentence to extract names from.
     * @return The specified sentence with all the names from the supplied spans extracted.
     */
    private def extractNames(spans: List[Span], sentence: List[Token.Text]): UIO[List[Token]] = spans match
      case head :: tail =>
        for _tail <- extractNames(tail, sentence) yield
          val begin = _tail take head.getStart
          val middle = NonEmptyList fromList _tail.slice(head.getStart, head.getEnd).collect {
            case t@Token.Text(_, _) => t
          }
          val end = _tail drop head.getEnd
          middle.fold(begin ::: end)(begin ::: Token.Name(_) :: end)
      case Nil => pure(sentence)
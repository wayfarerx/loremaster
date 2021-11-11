/* Analysis.scala
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

import java.io.InputStream
import java.net.{URI, URL}

import collection.immutable.SortedSet

import cats.data.NonEmptyList

import opennlp.tools.namefind.{NameFinderME, TokenNameFinder, TokenNameFinderModel}
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetector, SentenceDetectorME}
import opennlp.tools.tokenize.{
  Detokenizer,
  DetokenizationDictionary,
  DictionaryDetokenizer,
  TokenizerModel,
  Tokenizer,
  TokenizerME
}
import opennlp.tools.util.Span

import zio.{Has, RIO, Task, TaskManaged}

import model.*

/**
 * Definition of the analysis service API.
 */
trait Analysis:

  /**
   * Constructs lore from the specified book.
   *
   * @param book The book to construct lore from.
   * @return Lore constructed from the specified book.
   */
  def analyze(book: Book): Task[Option[Lore]]

  /**
   * Constructs a book from the specified lore.
   *
   * @param lore The lore to construct a book from.
   * @return A book constructed from the specified lore.
   */
  def render(lore: Lore): Task[Option[Book]]

/**
 * Definitions associated with analysis services.
 */
object Analysis:

  /** The live analysis layer. */
  val live: zio.RLayer[Has[Configuration] & Has[Log.Factory], Has[Analysis]] =
    zio.ZLayer fromEffect {
      for
        config <- RIO.service[Configuration]
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Analysis].getSimpleName)
        result <- apply(
          config.sentenceModel,
          config.tokenizerModel,
          config.detokenizerDictionary,
          config.partOfSpeechModel,
          config.personModel,
          config.locationModel,
          config.organizationModel,
          log
        )
      yield result
    }

  /**
   * Creates an analysis service.
   *
   * @param sentenceModel         The optional URI to download the NLP sentence model from.
   * @param tokenizerModel        The optional URI to download the NLP tokenizer model from.
   * @param detokenizerDictionary The optional URI to download the NLP detokenizer dictionary from.
   * @param partOfSpeechModel     The optional URI to download the NLP part of speech model from.
   * @param personModel           The optional URI to download the NLP person person name finder model from.
   * @param locationModel         The optional URI to download the NLP person location name finder model from.
   * @param organizationModel     The optional URI to download the NLP person organization name finder model from.
   * @param log                   The log to append to.
   * @return An analysis service.
   */
  def apply(
    sentenceModel: Option[URI],
    tokenizerModel: Option[URI],
    detokenizerDictionary: Option[URI],
    partOfSpeechModel: Option[URI],
    personModel: Option[URI],
    locationModel: Option[URI],
    organizationModel: Option[URI],
    log: Log
  ): Task[Analysis] = for
    _sentenceModel <- model(sentenceModel, "en-sent")
      .use(Task apply SentenceModel(_))
    _tokenizerModel <- model(tokenizerModel, "en-token")
      .use(Task apply TokenizerModel(_))
    _detokenizerDictionary <- dictionary(detokenizerDictionary, "latin-detokenizer")
      .use(Task apply DetokenizationDictionary(_))
    _partOfSpeechModel <- model(partOfSpeechModel, default = "en-pos-maxent")
      .use(Task apply POSModel(_))
    _personModel <- model(personModel, "en-ner-person")
      .use(Task apply TokenNameFinderModel(_))
    _locationModel <- model(locationModel, "en-ner-location")
      .use(Task apply TokenNameFinderModel(_))
    _organizationModel <- model(organizationModel, "en-ner-organization")
      .use(Task apply TokenNameFinderModel(_))
  yield Live(
    SentenceDetectorME(_sentenceModel),
    TokenizerME(_tokenizerModel),
    DictionaryDetokenizer(_detokenizerDictionary),
    POSTaggerME(_partOfSpeechModel),
    NameFinderME(_personModel),
    NameFinderME(_locationModel),
    NameFinderME(_organizationModel),
    log
  )

  /**
   * Loads an NLP dictionary from the specified URI or the default classpath resource.
   *
   * @param uri     The optional URI to load from.
   * @param default The name of the default classpath resource.
   * @return An NLP dictionary from the specified path, URI or the default classpath resource.
   */
  private def dictionary(uri: Option[URI], default: String): TaskManaged[InputStream] =
    load(uri, s"nlp/$default.xml")

  /**
   * Loads an NLP model from the specified URI or the default classpath resource.
   *
   * @param uri     The optional URI to load from.
   * @param default The name of the default classpath resource.
   * @return An NLP model from the specified path, URI or the default classpath resource.
   */
  private def model(uri: Option[URI], default: String): TaskManaged[InputStream] =
    load(uri, s"nlp/$default.bin")

  /**
   * Loads data from the specified URI or the fallback classpath resource.
   *
   * @param uri      The optional URI to load from.
   * @param fallback The location of the fallback classpath resource.
   * @return Data from the specified path, URI or the fallback classpath resource.
   */
  private def load(uri: Option[URI], fallback: String): TaskManaged[InputStream] =
    zio.ZManaged fromAutoCloseable uri.fold {
      for
        stream <- Task(Option(getClass.getClassLoader.getResourceAsStream(fallback)))
        result <- stream.fold(fail(s"Unable to load fallback analysis resource at $fallback."))(pure(_))
      yield result
    }(Task apply _.toURL flatMap (Task apply _.openStream))

  /**
   * Constructs lore from the specified book.
   *
   * @param book The book to construct lore from.
   * @return Lore constructed from the specified book.
   */
  inline def analyze(book: Book): RIO[Has[Analysis], Option[Lore]] =
    RIO.service flatMap (_ analyze book)

  /**
   * Constructs a book from the specified lore.
   *
   * @param lore The lore to construct a book from.
   * @return A book constructed from the specified lore.
   */
  inline def render(lore: Lore): RIO[Has[Analysis], Option[Book]] =
    RIO.service flatMap (_ render lore)

  /**
   * The live analysis implementation.
   *
   * @param sentenceDetector   The NLP sentence detector to use.
   * @param tokenizer          The NLP tokenizer to use.
   * @param detokenizer        The NLP detokenizer to use.
   * @param partOfSpeechTagger The part of speech tagger to use.
   * @param personFinder       The name finder to use.
   * @param log                The log to append to.
   */
  private case class Live(
    sentenceDetector: SentenceDetector,
    tokenizer: Tokenizer,
    detokenizer: Detokenizer,
    partOfSpeechTagger: POSTagger,
    personFinder: TokenNameFinder,
    locationFinder: TokenNameFinder,
    organizationFinder: TokenNameFinder,
    log: Log
  ) extends Analysis :

    import Live.*

    /* Construct lore from the specified book. */
    override def analyze(book: Book): Task[Option[Lore]] =
      analyzeParagraphs(book.toList) map (Lore.from(_ *))

    /* Construct a book from the specified lore. */
    override def render(lore: Lore): Task[Option[Book]] =
      renderParagraphs(lore.toList) map (Book.from(_ *))

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
      Option(paragraph).filterNot(_.isEmpty).fold(none) { _paragraph =>
        for
          sentences <- Task(sentenceDetector.sentDetect(_paragraph)) map (Option(_) filterNot (_.isEmpty))
          result <- sentences.fold(none)(analyzeSentences(_) flatMap some)
        yield result flatMap NonEmptyList.fromList
      }

    /**
     * Analyzes an array of sentences starting at the specified offset.
     *
     * @param sentences The sentences to analyze.
     * @param offset    The offset into the array to start at.
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
      Option(sentence).filterNot(_.isEmpty).fold(none) { _sentence =>
        for
          text <- Task(tokenizer.tokenize(_sentence)) map (Option(_) filterNot (_.isEmpty))
          result <- text.fold(none) { _text =>
            for
              partsOfSpeech <- Task(partOfSpeechTagger.tag(_text))
              personNames <- Task(personFinder find _text)
                .map(_.iterator map (Name(_, Token.Name.Category.Person)))
              locationNames <- Task(locationFinder find _text)
                .map(_.iterator map (Name(_, Token.Name.Category.Location)))
              organizationNames <- Task(organizationFinder find _text)
                .map(_.iterator map (Name(_, Token.Name.Category.Organization)))
              withNames <- extractNames(
                0,
                _text.iterator.zip(partsOfSpeech.iterator).flatMap { case (txt, pos) =>
                  Option(txt).filterNot(_.isEmpty).map(_txt => Token.Text(_txt, Option(pos) filterNot (_.isEmpty)))
                }.toList,
                (personNames ++ locationNames ++ organizationNames).toArray.sorted.foldLeft(SortedSet.empty[Name]) {
                  (set, name) =>
                    set.lastOption.fold(set + name) { last =>
                      if last.span.getEnd >= name.span.getEnd then set else
                        set + name.copy(Span(Math.max(last.span.getEnd, name.span.getStart), name.span.getEnd))
                    }
                }.toList
              )
            yield Option(withNames)
          }
        yield result flatMap NonEmptyList.fromList
      }

    /**
     * Extracts names from the specified sentence.
     *
     * @param cursor    The number of tokens that were dropped from the sentence.
     * @param remaining The remaining sentence to extract names from.
     * @param names     The names to extract.
     * @return The specified sentence with all the names extracted.
     */
    private def extractNames(
      cursor: Int,
      remaining: List[Token.Text],
      names: List[Name]
    ): Task[List[Token]] =
      names match
        case head :: tail =>
          val start = Math.max(0, head.span.getStart - cursor)
          val end = Math.max(start, head.span.getEnd - cursor)
          val prefix = remaining take start
          for
            _head <- NonEmptyList.fromList(remaining.slice(start, end)).fold(none) { tokens =>
              Task(detokenizer.detokenize(tokens.iterator.map(_.string).toArray, " "))
                .map(result => Option(result) filterNot (_.isEmpty))
            }
            _tail <- extractNames(cursor + end, remaining drop end, tail)
          yield _head.fold(prefix ::: _tail)(prefix ::: Token.Name(_, head.category) :: _tail)
        case Nil => pure(remaining)

    /**
     * Renders a list of paragraphs.
     *
     * @param paragraphs The paragraphs to render.
     * @return The rendered list of paragraphs.
     */
    private def renderParagraphs(paragraphs: List[Paragraph]): Task[List[String]] = paragraphs match
      case head :: tail =>
        for
          _head <- renderParagraph(head)
          _tail <- renderParagraphs(tail)
        yield _head.fold(_tail)(_ :: _tail)
      case Nil => nil

    /**
     * Renders a single paragraph.
     *
     * @param paragraph The paragraph to render.
     * @return The rendered paragraph.
     */
    private def renderParagraph(paragraph: Paragraph): Task[Option[String]] = for
      str <- Task(detokenizer.detokenize(paragraph.iterator.flatMap(_.iterator).map(_.toString).toArray, " "))
    yield Option(str) filterNot (_.isEmpty)

  /**
   * Factory for live analysis services.
   */
  private object Live extends ((
    SentenceDetector,
      Tokenizer,
      Detokenizer,
      POSTagger,
      TokenNameFinder,
      TokenNameFinder,
      TokenNameFinder,
      Log
    ) => Live) :

    /**
     * A named span in a list of tokens.
     *
     * @param span     The range of tokens to include.
     * @param category The category of the resulting name.
     */
    private case class Name(span: Span, category: Token.Name.Category)

    /**
     * Factory for named spans of tokens.
     */
    private object Name extends ((Span, Token.Name.Category) => Name) :

      /** The natural order of named spans. */
      given Ordering[Name] = (x, y) => x.span compareTo y.span match
        case 0 => x.category.ordinal - y.category.ordinal
        case nonZero => nonZero
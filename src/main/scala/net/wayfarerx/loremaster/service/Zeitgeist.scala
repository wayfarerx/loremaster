/* Zeitgeist.scala
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
import model.Book

/**
 * Definition of the zeitgeist service API.
 */
trait Zeitgeist:

  /** The lore locations that this zeitgeist provides by ID. */
  def list: Task[Map[ID, Location]]

  /**
   * Returns the lore at the specified Location if it exists.
   *
   * @param location The location of the lore to return.
   * @return The lore at the specified location if it exists.
   */
  def load(location: Location): Task[Option[Book]]

/**
 * Definitions associated with zeitgeist services.
 */
object Zeitgeist:

  import java.time.Instant
  import java.util.concurrent.TimeUnit
  import scala.concurrent.duration.*
  import zio.clock.Clock
  import zio.{Has, Ref, RIO, UIO, ZIO}

  /** The base storage ID for use by zeitgeist implementations. */
  val ZeitgeistId: ID = id"zeitgeist"

  /** The storage prefix for zeitegist implementations. */
  val Prefix: Location = Location.of(ZeitgeistId)

  /** The live zeitgeist layer. */
  val live: zio.RLayer[Clock & Has[Configuration] & Has[Log.Factory] & Has[Storage] & Has[Https], Has[Zeitgeist]] =
    zio.ZLayer.fromEffect {
      for
        clock <- RIO.service[Clock.Service]
        configuration <- RIO.service[Configuration]
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Zeitgeist].getSimpleName)
        storage <- RIO.service[Storage]
        https <- RIO.service[Https]
        result <- apply(
          configuration.zeitgeist,
          clock,
          log,
          storage,
          https,
          configuration.cacheExpiration,
          configuration.remoteCooldown
        )
      yield result
    }

  /** The supported zeitgeist factories. */
  private val factories: Map[String, (Clock.Service, Log, Storage, Https, FiniteDuration, FiniteDuration, Ref[Instant]) => Zeitgeist] =
    Map(TesImperialLibrary.Designator.toLowerCase -> TesImperialLibrary)

  /**
   * Creates an zeitgeist of the specified designator's type.
   *
   * @param designator The designator that identifies the type of zeitgeist to create.
   * @param clock      The clock service to use.
   * @param log        The log to append to.
   * @param storage    The storage service to use for caching.
   * @param https      The service that processes HTTPS operations.
   * @param expiration The cache expiration period to use.
   * @param cooldown   The amount of time to allow for between remote calls.
   * @return Ann zeitgeist of the specified designator's type.
   */
  def apply(
    designator: String,
    clock: Clock.Service,
    log: Log,
    storage: Storage,
    https: Https,
    expiration: FiniteDuration,
    cooldown: FiniteDuration
  ): Task[Zeitgeist] =
    factories.get(designator.toLowerCase).fold(fail(s"Invalid zeitgeist designator: $designator.")) { factory =>
      for
        start <- Task(Instant.ofEpochMilli(0L))
        ref <- Ref.make(start)
      yield factory(clock, log, storage, https, expiration, cooldown, ref)
    }

  /** The lore locations that the zeitgeist provides by ID. */
  inline def list: RIO[Has[Zeitgeist], Map[ID, Location]] =
    RIO.service flatMap (_.list)

  /**
   * Returns the lore at the specified location.
   *
   * @param location The location of the lore to return.
   * @return The lore at the specified location.
   */
  inline def load(location: Location): RIO[Has[Zeitgeist], Option[Book]] =
    RIO.service flatMap (_.load(location))

  /**
   * A zeitgeist that consults a website.
   */
  trait Website extends Zeitgeist :

    import java.util.concurrent.atomic.AtomicReference

    /** The clock service to use. */
    protected def clock: Clock.Service

    /** The log to append to. */
    protected def log: Log

    /** The storage service to use for caching. */
    protected def storage: Storage

    /** The service that processes HTTPS operations.. */
    protected def https: Https

    /** The cache expiration period to use. */
    protected def expiration: FiniteDuration

    /** The amount of time to allow for between remote calls. */
    protected def cooldown: FiniteDuration

    /** The instant the previous operation completed. */
    protected def previousCompletionAt: Ref[Instant]

    /** Returns the host to connect to. */
    protected def host: String

    /** Returns the location of the index document. */
    protected def index: Location

    /** The prefix of URIs that point to this website. */
    protected final lazy val prefix = s"https://$host/"

    /** The checkpoint to consult when cooling down remote requests. */
    private val checkpoint = AtomicReference(Instant.MIN)

    /* The lore locations that this zeitgeist provides. */
    final override val list: Task[Map[ID, Location]] = for
      webpage <- materialize(index)
      results <- webpage.fold(fail(s"Failed to materialize zeitgeist index: ${index.encoded}"))(parseIndex)
    yield results.foldLeft(Map.empty[ID, Location]) { (map, result) =>
      val reversed = result.reverse
      index(map, result, List(reversed.head), reversed.tail)
    } take 5 // FIXME Remove take 5

    /* Return the lore at the specified Location if it exists. */
    final override def load(location: Location): Task[Option[Book]] = for
      webpage <- materialize(location)
      result <- webpage.fold(none)(parseBook(location, _))
    yield result

    /**
     * Parses a webpage into a set of locations.
     *
     * @param webpage The content of the webpage to parse into locations.
     * @return The specified webpage parsed into a set of locations.
     */
    protected def parseIndex(webpage: String): Task[Set[Location]]

    /**
     * Parses a webpage into a book.
     *
     * @param at      The location of the webpage to parse.
     * @param webpage The content of the webpage to parse into a book.
     * @return The specified webpage parsed into a book.
     */
    protected def parseBook(at: Location, webpage: String): Task[Option[Book]]

    /**
     * Materializes the content at the specified location if it exists.
     *
     * @param location The location of the content to materialize.
     * @return The content at the specified location if it exists.
     */
    private def materialize(location: Location): Task[Option[String]] =
      val cached = Prefix :++ {
        if location.last.encoded endsWith ".html" then location
        else location :+ id"index.html"
      }
      for
        lastModified <- storage.lastModified(cached)
        result <- lastModified.fold(download(location, cached, None)) { lastModifiedAt =>
          for
            lastModifiedMillis <- Task(lastModifiedAt.toEpochMilli)
            thresholdMillis <- clock.currentTime(TimeUnit.MILLISECONDS) map (_ - expiration.toMillis)
            data <-
              if lastModifiedMillis <= thresholdMillis then download(location, cached, lastModified)
              else storage.load(cached)
          yield data
        }
      yield result

    /**
     * Downloads the content at the specified location if it exists.
     *
     * @param location     The location of the content to download.
     * @param cached       The location of the cached copy.
     * @param lastModified The optional last modified instant of the locally cached version.
     * @return The content at the specified location if it exists.
     */
    private def download(
      location: Location,
      cached: Location,
      lastModified: Option[Instant]
    ): Task[Option[String]] = for
      _ <- allowForCooldown
      response <- https.get(host, location, lastModified)
      downloadedAt <- resetCooldown
      result <- response match
        case Https.Response.NotFound =>
          lastModified.fold(none)(_ => storage.delete(cached) map (_ => None))
        case Https.Response.NotModified =>
          lastModified.fold(none)(_ => storage.touch(cached, downloadedAt) flatMap (_ => storage.load(cached)))
        case Https.Response.Content(text) =>
          storage.save(cached, text) map (_ => Some(text))
    yield result

    /**
     * Allows for the cooldown to expire.
     *
     * @return The current instant after allowing for the cooldown to expire.
     */
    private def allowForCooldown: Task[Unit] = for
      previousAt <- previousCompletionAt.get
      previousAtMillis <- Task(previousAt.toEpochMilli)
      nowMillis <- clock currentTime TimeUnit.MILLISECONDS
      _ <- {
        val sleepForMillis = previousAtMillis + cooldown.toMillis - nowMillis
        if sleepForMillis <= 0L then unit else {
          for
            _ <- log.debug(s"Cooling down for $sleepForMillis milliseconds.")
            _ <- clock.sleep(zio.duration.Duration.fromMillis(sleepForMillis))
          yield ()
        }
      }
    yield ()

    /**
     * Resets the cooldown counter to the current time.
     *
     * @return The current instant that the counter was reset to.
     */
    private def resetCooldown: Task[Instant] = for
      nowMillis <- clock currentTime TimeUnit.MILLISECONDS
      now <- Task(Instant.ofEpochMilli(nowMillis))
      _ <- previousCompletionAt.set(now)
    yield now

    /**
     * Adds the specified location to a map.
     *
     * @param map       The map to add the location to.
     * @param location  The location to add to the map.
     * @param candidate The candidate ID sequence to attempt.
     * @param remaining The remaining ID prefixes to try.
     * @return The specified location added to the map.
     */
    @annotation.tailrec
    private def index(
      map: Map[ID, Location],
      location: Location,
      candidate: List[ID],
      remaining: List[ID]
    ): Map[ID, Location] =
      ID.decode(candidate mkString "__") match {
        case Some(id) if !map.contains(id) => map + (id -> location)
        case _ => index(map, location, remaining.head :: candidate, remaining.tail)
      }

  /**
   * The Elder Scrolls Imperial Library zeitgeist.
   *
   * @param clock                The clock service to use.
   * @param log                  The log to append to.
   * @param storage              The storage service to use for caching.
   * @param https                The service that processes HTTPS operations.
   * @param expiration           The cache expiration period to use.
   * @param cooldown             The amount of time to allow for between remote calls.
   * @param previousCompletionAt The instant the previous operation completed.
   */
  case class TesImperialLibrary(
    clock: Clock.Service,
    log: Log,
    storage: Storage,
    https: Https,
    expiration: FiniteDuration,
    cooldown: FiniteDuration,
    previousCompletionAt: Ref[Instant]
  ) extends Website :

    import scala.jdk.CollectionConverters.*
    import cats.data.NonEmptyList
    import org.jsoup.Jsoup
    import org.jsoup.nodes.{Document, Element}
    import org.jsoup.select.Elements
    import TesImperialLibrary.*

    /* The host to connect to. */
    override protected def host = Host

    /* The location of the index document. */
    override protected def index = Index

    /* Parse a webpage into a collection of locations. */
    override protected def parseIndex(webpage: String) = for
      document <- parse(index, webpage)
      results <- allResults(document, ".views-field-title a[href]") { element =>
        Option(element attr "abs:href") collect {
          case prefixed if prefixed startsWith prefix => prefixed drop prefix.size
        } flatMap Location.decode
      }
    yield results

    /* Parse a webpage into lore. */
    override protected def parseBook(at: Location, webpage: String) = for
      document <- parse(at, webpage)
      title <- firstResult(document, "#main .page-title") { element =>
        Option(element.text) filterNot (_.isEmpty)
      }
      author <- firstResult(document, "#content .field-field-author .field-item") { element =>
        Option(element.ownText.trim) filterNot (_.isEmpty)
      }
      content <- firstResult(document, "#content .prose") { element =>
        Option(element.children) map { children =>
          children.iterator.asScala flatMap { child =>
            child.tagName match
              case "p" | "div" if child.classNames.isEmpty => Option(child.text) filterNot (_.isEmpty)
              case _ => None
          }
        } filterNot (_.isEmpty)
      }
    yield NonEmptyList fromList content.fold(Nil)(_.toList) map (Book(title, author, _))

    /**
     * Parses the document at a location.
     *
     * @param location The location of the document to parse.
     * @param webpage  The content of the document to parse.
     * @return The parsed document.
     */
    private def parse(location: Location, webpage: String): Task[Document] =
      Task(Jsoup.parse(webpage, s"$prefix${location.encoded}/"))

    /**
     * Returns the first result of a CSS query.
     *
     * @tparam T The type of result to return.
     * @param document The document to search.
     * @param cssQuery The CSS query to use.
     * @param f        A function that extracts a result from an element.
     * @return The first result of a CSS query.
     */
    private def firstResult[T](document: Document, cssQuery: String)(f: Element => Option[T]): Task[Option[T]] =
      for result <- Task(document selectFirst cssQuery) yield Option(result) flatMap f

    /**
     * Returns all the results of a CSS query.
     *
     * @tparam T The type of result to return.
     * @param document The document to search.
     * @param cssQuery The CSS query to use.
     * @param f        A function that extracts a result from an element.
     * @return All the results of a CSS query.
     */
    private def allResults[T](document: Document, cssQuery: String)(f: Element => Option[T]): Task[Set[T]] =
      for results <- Task(document select cssQuery) yield results.iterator.asScala.flatMap(f).toSet

  /**
   * Factory for Elder Scrolls Imperial Zeitgeist services.
   */
  object TesImperialLibrary
    extends ((Clock.Service, Log, Storage, Https, FiniteDuration, FiniteDuration, Ref[Instant]) => TesImperialLibrary) :

    /** The designator for the TES Imperial Library zeitgeist. */
    val Designator = "TesImperialLibrary"

    /** The TES Imperial Library host to connect to. */
    private val Host = "www.imperial-library.info"

    /** The location of the TES Imperial Library index document. */
    private val Index = location"books/all/by-title"
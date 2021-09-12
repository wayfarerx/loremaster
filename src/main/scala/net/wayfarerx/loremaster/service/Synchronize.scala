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

/**
 * Definition of the synchronize service API.
 */
trait Synchronize:

  /**
   * Synchronizes the content of the zeitgeist on to the library.
   *
   * @return True if the library was updated.
   */
  def sync(): Task[Boolean]

/**
 * Definitions associated with synchronize services.
 */
object Synchronize:

  import java.time.Instant
  import java.util.concurrent.TimeUnit
  import scala.concurrent.duration.FiniteDuration
  import zio.{Has, RIO, UIO}
  import model._

  /** The live synchronize layer. */
  val live: zio.RLayer[
    Has[Configuration] & Has[Log.Factory] & Has[Zeitgeist] & Has[Analysis] & Has[Library],
    Has[Synchronize]
  ] = zio.ZLayer.fromEffect {
    for
      configuration <- RIO.service[Configuration]
      logFactory <- RIO.service[Log.Factory]
      log <- logFactory(classOf[Synchronize].getSimpleName)
      zeitgeist <- RIO.service[Zeitgeist]
      analysis <- RIO.service[Analysis]
      library <- RIO.service[Library]
    yield apply(configuration.syncLimit, log, zeitgeist, analysis, library)
  }

  /**
   * Creates an zeitgeist of the specified designator's type.
   *
   * @param limit     The maximum number of entries to synchronize in a single pass.
   * @param log       The log to append to.
   * @param zeitgeist The storage service to use.
   * @param analysis  The analysis service to use.
   * @param library   The library service to use.
   * @return Ann zeitgeist of the specified designator's type.
   */
  def apply(
    limit: Int,
    log: Log,
    zeitgeist: Zeitgeist,
    analysis: Analysis,
    library: Library
  ): Synchronize =
    Live(limit, log, zeitgeist, analysis, library)

  /**
   * Mirrors the content of the zeitgeist on to the library.
   *
   * @return True if the library was modified.
   */
  inline def sync(): RIO[Has[Synchronize], Boolean] = RIO.service flatMap (_.sync())

  /**
   * The live synchronize implementation.
   *
   * @param limit     The maximum number of entries to download in a single pass.
   * @param clock     The clock service to use.
   * @param log       The log to append to.
   * @param zeitgeist The zeitgeist to pull from.
   * @param analysis  The analysis service to use.
   * @param library   The library to push to.
   */
  private case class Live(
    limit: Int,
    log: Log,
    zeitgeist: Zeitgeist,
    analysis: Analysis,
    library: Library
  ) extends Synchronize :

    import collection.immutable.SortedSet
    import Live._

    /* Synchronize the content of the zeitgeist on to the library. */
    override def sync(): Task[Boolean] = for
      books <- zeitgeist.list
      lore <- library.list
      result <-
        val (known, unknown) = books partition { case (id, _) => lore(id) }
        for
          deleted <- deleteRemoved(lore -- books.keySet)
          created <- createAdded(unknown)
          updated <-
            val remaining = limit - unknown.size
            if remaining <= 0 then pure(false) else updateExisting(known, remaining)
        yield deleted || created || updated
    yield result

    /**
     * Creates lore from books that have been added to the zeitgeist.
     *
     * @param entries The entries to create.
     * @return True if the library was modified.
     */
    private def createAdded(entries: Map[ID, Location]): Task[Boolean] =

      def createInLibrary(iterator: Iterator[(ID, Location)]): Task[Boolean] =
        if iterator.hasNext then
          val (id, location) = iterator.next
          for
            _ <- log.debug(s"Creating library entry ${id.encoded}.")
            head <- sync(id, location)
            tail <- createInLibrary(iterator)
          yield head || tail
        else pure(false)

      createInLibrary(entries.iterator take limit)

    /**
     * Updates existing lore from the zeitgeist according to the configuration.
     *
     * @param entries     The entries to potentially update.
     * @param updateLimit The maximum number of updates to perform.
     * @return True if the library was modified.
     */
    private def updateExisting(entries: Map[ID, Location], updateLimit: Int): Task[Boolean] =

      def collectCandidates(iterator: Iterator[(ID, Location)]): Task[SortedSet[Candidate]] =
        if iterator.hasNext then
          val (id, location) = iterator.next
          for
            head <- library.lastModified(id)
            tail <- collectCandidates(iterator)
          yield head.fold(tail)(tail + Candidate(id, location, _) take updateLimit)
        else pure(SortedSet.empty)

      def updateInLibrary(iterator: Iterator[Candidate]): Task[Boolean] =
        if iterator.hasNext then
          val candidate = iterator.next
          for
            _ <- log.debug(s"Updating library entry ${candidate.id.encoded}.")
            head <- sync(candidate.id, candidate.location)
            tail <- updateInLibrary(iterator)
          yield head || tail
        else pure(false)

      for
        candidates <- collectCandidates(entries.iterator)
        result <- updateInLibrary(candidates.iterator)
      yield result

    /**
     * Deletes lore from the library that are not found in the zeitgeist.
     *
     * @param ids The IDs of the lore to delete.
     * @return True if the library was modified.
     */
    private def deleteRemoved(ids: Set[ID]): Task[Boolean] =

      def deleteFromLibrary(iterator: Iterator[ID]): Task[Boolean] =
        if iterator.hasNext then
          val id = iterator.next
          for
            _ <- log.debug(s"Deleting library entry ${id.encoded}.")
            exists <- library.exists(id)
            head <- if exists then library.delete(id) map (_ => true) else pure(false)
            tail <- deleteFromLibrary(iterator)
          yield head || tail
        else pure(false)

      deleteFromLibrary(ids.iterator)

    /**
     * Ensures that the local lore mirrors the specified remote book.
     *
     * @param id       The ID of the lore to synchronize.
     * @param location The location of the book that describes the lore.
     * @return True if the library was modified.
     */
    private def sync(id: ID, location: Location): Task[Boolean] = for
      content <- zeitgeist.load(location)
      result <- content match
        case Some(book) =>
          for
            lore <- analysis.analyze(book)
            update <- lore match
              case Some(_lore) =>
                for
                  previous <- library.load(id)
                  updated <- library.save(id, _lore) map (_ => previous.fold(true)(_lore != _))
                yield updated
              case None => UIO(false)
          yield update
        case None =>
          for
            exists <- library.exists(id)
            deleted <- if exists then library.delete(id) map (_ => true) else pure(false)
          yield deleted
    yield result

  /**
   * Factory for live synchronize services.
   */
  private object Live extends ((Int, Log, Zeitgeist, Analysis, Library) => Live) :

    /**
     * An entry that is a candidate for being updated.
     *
     * @param id           The ID of the entry.
     * @param location     The location of the entry.
     * @param lastModified The instant the entry was last modified in the library.
     */
    private case class Candidate(id: ID, location: Location, lastModified: Instant)

    /**
     * Factory for entry candidates.
     */
    private object Candidate extends ((ID, Location, Instant) => Candidate) :

      /** The ordering of entry candidates. */
      given Ordering[Candidate] = (x: Candidate, y: Candidate) =>
        x.lastModified.compareTo(y.lastModified) match
          case 0 => Ordering[ID].compare(x.id, y.id)
          case nonZero => nonZero
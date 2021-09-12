/* Library.scala
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

import java.time.Instant
import zio.Task
import model.Lore

/**
 * Definition of the library service API.
 */
trait Library:

  /**
   * Lists the IDs of the lore this library contains.
   *
   * @return The IDs of the lore this library contains.
   */
  def list: Task[Set[ID]]

  /**
   * Returns true if this library contains lore with the specified ID.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return true if this library contains lore with the specified ID.
   */
  def exists(id: ID): Task[Boolean]

  /**
   * Returns the instant that the specified lore was last modified if it exists.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return the instant that the specified lore was last modified if it exists.
   */
  def lastModified(id: ID): Task[Option[Instant]]

  /**
   * Updates the last modified instant of the specified lore if it exists.
   *
   * @param id           The ID of the lore to touch.
   * @param lastModified The instant to set as the last modified time.
   */
  def touch(id: ID, lastModified: Instant): Task[Unit]

  /**
   * Loads the lore with the specified ID.
   *
   * @param id The ID of the lore to load.
   * @return The lore with the specified ID.
   */
  def load(id: ID): Task[Option[Lore]]

  /**
   * Saves a lore using the specified ID.
   *
   * @param id   The ID of the lore to save.
   * @param lore The lore to save.
   */
  def save(id: ID, lore: Lore): Task[Unit]

  /**
   * Deletes the lore with the specified ID.
   *
   * @param id The ID of the lore to delete.
   */
  def delete(id: ID): Task[Unit]

/**
 * Definitions associated with library services.
 */
object Library:

  import zio.{Has, RIO}

  /** The base storage ID for use by library implementations. */
  val LibraryId: ID = id"library"

  /** The storage prefix for live library implementations. */
  val Prefix: Location = Location.of(LibraryId)

  /** The live library layer. */
  val live: zio.RLayer[Has[Log.Factory] & Has[Storage], Has[Library]] =
    zio.ZLayer.fromEffect {
      for
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Library].getSimpleName)
        storage <- RIO.service[Storage]
      yield apply(log, storage)
    }

  /**
   * Creates a library service that uses the specified storage.
   *
   * @param log     The log to append to.
   * @param storage The storage to use.
   * @return A library service that uses the specified storage.
   */
  def apply(log: Log, storage: Storage): Library = Live(log, storage)

  /**
   * Lists the IDs of the lore the library contains.
   *
   * @return The IDs of the lore the library contains.
   */
  inline def list: RIO[Has[Library], Set[ID]] =
    RIO.service flatMap (_.list)

  /**
   * Returns true if the library contains lore with the specified ID.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return true if the library contains lore with the specified ID.
   */
  inline def exists(id: ID): RIO[Has[Library], Boolean] =
    RIO.service flatMap (_.exists(id))

  /**
   * Returns the instant that the specified lore was last modified if it exists.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return the instant that the specified lore was last modified if it exists.
   */
  inline def lastModified(id: ID): RIO[Has[Library], Option[Instant]] =
    RIO.service flatMap (_.lastModified(id))

  /**
   * Updates the last modified instant of the specified lore if it exists.
   *
   * @param id           The ID of the lore to touch.
   * @param lastModified The instant to set as the last modified time.
   */
  inline def touch(id: ID, lastModified: Instant): RIO[Has[Library], Unit] =
    RIO.service flatMap (_.touch(id, lastModified))

  /**
   * Loads the lore with the specified ID.
   *
   * @param id The ID of the lore to load.
   * @return The lore with the specified ID.
   */
  inline def load(id: ID): RIO[Has[Library], Option[Lore]] =
    RIO.service flatMap (_.load(id))

  /**
   * Saves a lore using the specified ID.
   *
   * @param id   The ID of the lore to save.
   * @param lore The lore to save.
   */
  inline def save(id: ID, lore: Lore): RIO[Has[Library], Unit] =
    RIO.service flatMap (_.save(id, lore))

  /**
   * Deletes the lore with the specified ID.
   *
   * @param id The ID of the lore to delete.
   */
  inline def delete(id: ID): RIO[Has[Library], Unit] =
    RIO.service flatMap (_.delete(id))

  /**
   * The live library implementation.
   *
   * @param log     The log to append to.
   * @param storage The storage to use.
   */
  private case class Live(log: Log, storage: Storage) extends Library :

    import io.circe.generic.auto.*
    import io.circe.parser.decode
    import io.circe.syntax.*
    import Live.*

    /* List the IDs of the lore this library contains. */
    override def list =
      for children <- storage.list(Prefix) yield
        children.iterator.map(_.last.encoded).flatMap { encoded =>
          if (encoded endsWith Suffix) ID.decode(encoded dropRight Suffix.length) else None
        }.toSet

    /* Return true if this library contains lore with the specified ID. */
    override def exists(id: ID): Task[Boolean] = for
      storageId <- toStorageId(id)
      result <- storage.exists(Prefix :+ storageId)
    yield result

    /* Return the instant that the specified lore was last modified if it exists. */
    override def lastModified(id: ID): Task[Option[Instant]] = for
      storageId <- toStorageId(id)
      result <- storage.lastModified(Prefix :+ storageId)
    yield result

    /* Update the last modified instant of the specified lore if it exists. */
    override def touch(id: ID, lastModified: Instant): Task[Unit] = for
      storageId <- toStorageId(id)
      _ <- storage.touch(Prefix :+ storageId, lastModified)
    yield ()

    /* Load the lore with the specified ID. */
    override def load(id: ID) = for
      storageId <- toStorageId(id)
      result <- for
        json <- storage.load(Prefix :+ storageId)
        loaded <- json.fold(none)(decode[Lore](_).fold(fail(s"Failed to parse lore: ${id.encoded}", _), some))
      yield loaded
    yield result

    /* Save a lore using the specified ID. */
    override def save(id: ID, lore: Lore) = for
      storageId <- toStorageId(id)
      _ <- storage.save(Prefix :+ storageId, lore.asJson.spaces2)
    yield ()

    /* Delete the lore with the specified ID. */
    override def delete(id: ID) = for
      storageId <- toStorageId(id)
      _ <- storage.delete(Prefix :+ storageId)
    yield ()

  /**
   * Factory for live library services.
   */
  private object Live extends ((Log, Storage) => Live) :

    import cats.data.NonEmptyList

    /** The storage suffix used by live implementations. */
    val Suffix: String = ".json"

    /**
     * Converts a Library ID into a Storage ID.
     *
     * @param id The Library ID to convert into a Storage ID.
     * @return The Storage ID derived from the specified Library ID.
     */
    def toStorageId(id: ID): Task[ID] =
      ID.decode(id.encoded + Suffix).fold(fail(s"Invalid Library ID: ${id.encoded}."))(pure)
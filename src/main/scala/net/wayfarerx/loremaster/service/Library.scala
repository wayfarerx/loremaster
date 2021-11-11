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

import cats.data.NonEmptyList

import zio.{Has, RIO, Task}

import model.*
import json.given

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
  def touch(id: ID, lastModified: Instant = Instant.now): Task[Unit]

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

  /** The base storage prefix used bya library implementations. */
  val Prefix: ID = id"library"

  /** The live library layer. */
  val live: zio.RLayer[Has[Storage], Has[Library]] =
    zio.ZLayer fromEffect (RIO.service[Storage] map apply)

  /**
   * Creates a library service that uses the specified storage.
   *
   * @param storage The storage to use.
   * @return A library service that uses the specified storage.
   */
  def apply(storage: Storage): Library = Live(storage)

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
   * @param storage The storage to use.
   */
  private case class Live(storage: Storage) extends Library :

    import Live.*

    /* List the IDs of the lore this library contains. */
    override def list =
      storage list Location.of(Prefix) map (_ flatMap fromStorageLocation)

    /* Return true if this library contains lore with the specified ID. */
    override def exists(id: ID): Task[Boolean] =
      toStorageLocation(id).fold(pure(false))(storage.exists)

    /* Return the instant that the specified lore was last modified if it exists. */
    override def lastModified(id: ID): Task[Option[Instant]] =
      toStorageLocation(id).fold(none)(storage.lastModified)

    /* Update the last modified instant of the specified lore if it exists. */
    override def touch(id: ID, lastModified: Instant): Task[Unit] =
      toStorageLocation(id).fold(fail(s"Failed to touch $id in the library."))(storage.touch(_, lastModified))

    /* Load the lore with the specified ID. */
    override def load(id: ID) =
      toStorageLocation(id).fold(fail(s"Failed to load $id from the library.")) {
        storage load _ flatMap (_.fold(none)(json.parse[Lore](_) map Some.apply))
      }

    /* Save a lore using the specified ID. */
    override def save(id: ID, lore: Lore) =
      toStorageLocation(id).fold(fail(s"Failed to save $id to the library.")) { location =>
        json emit lore flatMap (storage.save(location, _))
      }

    /* Delete the lore with the specified ID. */
    override def delete(id: ID) =
      toStorageLocation(id).fold(fail(s"Failed to delete $id from the library."))(storage.delete)

  /**
   * Factory for live library services.
   */
  private object Live extends (Storage => Live) :

    /** The storage suffix used by live implementations. */
    private[this] val Suffix: String = ".json"

    /**
     * Extracts a library ID from a storage location.
     *
     * @param storageLocation The storage location to extract a library ID from.
     * @return The library ID extracted from the specified storage location.
     */
    private def fromStorageLocation(storageLocation: Location): Option[ID] =
      if storageLocation.size == 2 && storageLocation.head == Prefix then
        val last = storageLocation.last.value
        if last endsWith Suffix then ID fromString last.dropRight(Suffix.length) else None
      else None

    /**
     * Converts a library ID into a storage location.
     *
     * @param libraryId The library ID to convert into a storage location.
     * @return The storage location derived from the specified library ID.
     */
    private def toStorageLocation(libraryId: ID): Option[Location] =
      ID fromString libraryId.value + Suffix map (Location.of(Prefix, _))
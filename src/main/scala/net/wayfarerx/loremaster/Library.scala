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

import java.time.Instant
import zio.Task

/**
 * Definition of the library service API.
 */
trait Library:

  /**
   * Returns true if this library contains lore with the specified ID.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return true if this library contains lore with the specified ID.
   */
  def exists(id: String): Task[Boolean]

  /**
   * Returns the instant that the specified lore was last modified if it exists.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return the instant that the specified lore was last modified if it exists.
   */
  def lastModified(id: String): Task[Option[Instant]]

  /**
   * Loads lore from this library if it exists.
   *
   * @param id The ID of the lore to load.
   * @return The result of attempting to load lore from this library if it exists.
   */
  def load(id: String): Task[Option[Lore]]

  /**
   * Saves lore in this library.
   *
   * @param id    The ID of the lore to save.
   * @param lore The lore to save.
   * @return The result of attempting to save lore in this library.
   */
  def save(id: String, lore: Lore): Task[Unit]

  /**
   * Deletes lore from this library.
   *
   * @param id The ID of the lore to delete.
   * @return The result of attempting to delete lore from this library.
   */
  def delete(id: String): Task[Unit]

  /** Returns the IDs of the entries in this library. */
  def list: Task[Set[String]]

/**
 * Definitions associated with library services.
 */
object Library:

  import zio.{Has, RIO, RLayer, UIO, ZLayer}

  /** The live library layer. */
  val live: RLayer[Has[Storage], Has[Library]] =
    ZLayer.fromEffect(RIO.service flatMap apply)

  /**
   * Creates a library that uses the specified storage service.
   *
   * @param storage The storage service to use.
   * @return A library that uses the specified storage service.
   */
  def apply(storage: Storage): Task[Library] = UIO(Live(storage))

  /**
   * Returns true if the library contains lore with the specified ID.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return true if the library contains lore with the specified ID.
   */
  inline def exists(id: String): RIO[Has[Library], Boolean] = RIO.service flatMap (_.exists(id))

  /**
   * Returns the instant that the specified lore was last modified if it exists.
   *
   * @param id The ID of the lore to look for.
   * @return The result of attempting to return the instant that the specified lore was last modified if it exists.
   */
  inline def lastModified(id: String): RIO[Has[Library], Option[Instant]] = RIO.service flatMap (_.lastModified(id))

  /**
   * Loads lore from the library.
   *
   * @param id The ID of the lore to load.
   * @return The result of attempting to load lore from the library.
   */
  inline def load(id: String): RIO[Has[Library], Option[Lore]] = RIO.service flatMap (_.load(id))

  /**
   * Saves lore to the library.
   *
   * @param id    The ID of the lore to save.
   * @param lore The lore to save to the library.
   * @return The result of attempting to save lore to the library.
   */
  inline def save(id: String, lore: Lore): RIO[Has[Library], Unit] = RIO.service flatMap (_.save(id, lore))

  /**
   * Deletes lore from the library.
   *
   * @param id The ID of the lore to delete.
   * @return The result of attempting to delete lore from the library.
   */
  inline def delete(id: String): RIO[Has[Library], Unit] = RIO.service flatMap (_.delete(id))

  /** Returns the IDs of the entries in the library. */
  inline def list: RIO[Has[Library], Set[String]] = RIO.service flatMap (_.list)

  /**
   * A library service that uses storage.
   *
   * @param storage The storage to use.
   */
  private case class Live(storage: Storage) extends Library :

    import io.circe.generic.auto._
    import io.circe.syntax._
    import Live._

    /* Return true if this library contains lore with the specified ID. */
    override def exists(id: String) =
      storage exists resolve(id)

    /* Return the instant that the specified lore was last modified if it exists. */
    override def lastModified(id: String) =
      storage lastModified resolve(id)

    /* Load lore from this library if it exists. */
    override def load(id: String) = for
      text <- storage load resolve(id)
      lore <- text match
        case Some(json) =>
          for
            decoded <- Task(io.circe.parser.decode[Lore](json))
            result <- decoded.fold(fail(_), UIO.some)
          yield result
        case None => UIO.none
    yield lore

    /* Save lore in this library. */
    override def save(id: String, lore: Lore) = for
      text <- Task(Lore(
        lore.title map normalize filter nonEmpty,
        lore.author map normalize filter nonEmpty,
        lore.paragraphs.iterator.map(normalize).filter(nonEmpty).toList
      ).asJson.noSpaces)
      _ <- storage.save(resolve(id), text)
    yield ()

    /* Delete lore from this library. */
    override def delete(id: String) =
      storage delete resolve(id)

    /* Return the IDs of the entries in this library. */
    override def list = for listed <- storage list Prefix yield listed collect {
      case file if file endsWith Suffix => file.substring(Prefix.length, file.length - Suffix.length)
    }

  /**
   * Definitions associated with stored library services.
   */
  private object Live extends (Storage => Live) :

    /** The prefix used for storing library entries. */
    private val Prefix = "library/lore/"

    /** The suffix used for storing library entries. */
    private val Suffix = ".json"

    /** A function that identifies non-empty strings. */
    private val nonEmpty: String => Boolean = _.nonEmpty

    /** A function that trims and normalizes whitespace. */
    private val normalize: String => String = _.trim.replaceAll("\\s+", " ")

    /**
     * Returns the name of the file to store entries with the specified ID in.
     *
     * @param id The ID of the lore to resolve.
     * @return The name of the file to store entries with the specified ID in.
     */
    inline private def resolve(id: String): String = s"$Prefix$id$Suffix"
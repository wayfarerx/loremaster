/* Database.scala
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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration

import scala.concurrent.duration.FiniteDuration

import zio.{Has, Ref, RIO, RLayer, RManaged, Task, TaskManaged, UIO, ZLayer, ZManaged}
import zio.cache.{Cache, Lookup}

import model.*
import json.given

/**
 * Definition of the database service API.
 */
trait Database extends Database.Connection :

  /**
   * Returns a managed connection to this database.
   *
   * @return A managed connection to this database.
   */
  def connection: TaskManaged[Database.Connection] = Database.Connection(this)

/**
 * Definitions associated with database services.
 */
object Database:

  /** The storage prefix for database implementations. */
  val Prefix: Location = location"database"

  /** The live database layer. */
  val live: RLayer[Has[Configuration] & Has[Storage], Has[Database]] =
    ZLayer fromEffect {
      for
        configuration <- RIO.service[Configuration]
        storage <- RIO.service[Storage]
        result <- apply(configuration, storage)
      yield result
    }

  /** A managed a connection to the database. */
  val connection: RManaged[Has[Database], Connection] =
    ZManaged.service flatMap (_.connection)

  /**
   * Creates a live database service.
   *
   * @param configuration The configuration service to use.
   * @param storage       The storage service to use.
   * @return A live database service.
   */
  def apply(configuration: Configuration, storage: Storage): UIO[Database] =
    Live(configuration.databaseCacheSize, configuration.databaseCacheExpiration, storage)

  /**
   * Loads a table from this database.
   *
   * @param key The key of the table to load.
   * @return A table loaded from this database.
   */
  inline def load(key: Key): RIO[Has[Database], Option[Table]] =
    RIO.service flatMap (_.load(key))

  /**
   * Saves a table to this database.
   *
   * @param key   The key of the table to save.
   * @param table The table to save to this database.
   * @return The result of saving a table to this database.
   */
  inline def save(key: Key, table: Table): RIO[Has[Database], Unit] =
    RIO.service flatMap (_.save(key, table))

  /**
   * Deletes a table from this database.
   *
   * @param key The key of the table to delete.
   * @return The result of deleting a table from this database.
   */
  inline def delete(key: Key): RIO[Has[Database], Unit] =
    RIO.service flatMap (_.delete(key))

  /**
   * A connection to a database.
   */
  trait Connection:

    /**
     * Loads the specified table from the database if it exists.
     *
     * @param key The key that identifies the table to load.
     * @return The specified table loaded from the database if it exists.
     */
    def load(key: Key): Task[Option[Table]]

    /**
     * Saves the specified table to the database.
     *
     * @param key   The key that identifies the table to save.
     * @param table The specified table to save.
     * @return The result of attempting to save the specified table to the database.
     */
    def save(key: Key, table: Table): Task[Unit]

    /**
     * Deletes the specified table from the database.
     *
     * @param key The key that identifies the table to delete.
     * @return The result of attempting to delete the specified table from the database.
     */
    def delete(key: Key): Task[Unit]

  /**
   * Definitions associated with database connections.
   */
  object Connection:

    /**
     * Creates a connection to the specified database.
     *
     * @param database The database to connect to.
     * @return A connection to the specified database.
     */
    def apply(database: Database): TaskManaged[Connection] =
      ZManaged.make(Ref make Live.State(Map.empty, Set.empty) map (Live(database, _)))(_.commit().orDie)

    /**
     * Loads the specified table from the database if it exists.
     *
     * @param key The key that identifies the table to load.
     * @return The specified table loaded from the database if it exists.
     */
    inline def load(key: Key): RIO[Has[Connection], Option[Table]] =
      RIO.service flatMap (_.load(key))

    /**
     * Saves the specified table to the database.
     *
     * @param key   The key that identifies the table to save.
     * @param table The specified table to save.
     * @return The result of attempting to save the specified table to the database.
     */
    inline def save(key: Key, table: Table): RIO[Has[Connection], Unit] =
      RIO.service flatMap (_.save(key, table))

    /**
     * Deletes the specified table from the database.
     *
     * @param key The key that identifies the table to delete.
     * @return The result of attempting to delete the specified table from the database.
     */
    inline def delete(key: Key): RIO[Has[Connection], Unit] =
      RIO.service flatMap (_.delete(key))

    /**
     * The live database connection implementation.
     *
     * @param database The database that this connection targets.
     * @param state    The state of this connection.
     */
    private final class Live(database: Database, state: Ref[Live.State]) extends Connection :

      import Live.*

      /* Load the specified table from the database if it exists. */
      override def load(key: Key): Task[Option[Table]] = for
        currently <- state.get
        result <- if currently deleted key then none else currently.saved.get(key).fold(database load key)(some)
      yield result

      /* Save the specified table to the database. */
      override def save(key: Key, table: Table): Task[Unit] =
        state.update(_.save(key, table))

      /* Delete the specified table from the database. */
      override def delete(key: Key): Task[Unit] =
        state.update(_.delete(key))

      /**
       * Commits the operations on this connection.
       *
       * @return The result of committing the operations on this connection.
       */
      def commit(): Task[Unit] =

        def continue(remaining: State): Task[Unit] = remaining match
          case State(_, deleted) if deleted.nonEmpty =>
            val key = deleted.head
            database.delete(key) flatMap (_ => continue(remaining.copy(deleted = deleted - key)))
          case State(saved, _) if saved.nonEmpty =>
            val (key, table) = saved.head
            database.save(key, table) flatMap (_ => continue(remaining.copy(saved = saved - key)))
          case _ =>
            unit

        state modify (_ -> Live.State(Map.empty, Set.empty)) flatMap continue

    /**
     * Definitions associated with live database connections.
     */
    private object Live:

      /**
       * The state of a database connection.
       *
       * @param saved   The tables that should be saved.
       * @param deleted The tables that should be deleted.
       */
      case class State(saved: Map[Key, Table], deleted: Set[Key]):

        /**
         * Returns this state with the specified entry saved.
         *
         * @param key   The key of the entry to save.
         * @param table The table to save.
         * @return This state with the specified entry saved.
         */
        inline def save(key: Key, table: Table): State = State(saved + (key -> table), deleted - key)

        /**
         * Returns this state with the specified entry deleted.
         *
         * @param key The key of the entry to delete.
         * @return This state with the specified entry deleted.
         */
        inline def delete(key: Key): State = State(saved - key, deleted + key)

      /**
       * Definitions associated with database states.
       */
      object State extends ((Map[Key, Table], Set[Key]) => State) :

        /** The empty database state. */
        val empty: State = State(Map.empty, Set.empty)

  /**
   * The live database implementation.
   *
   * @param cache   The cache to use.
   * @param storage The storage service to use.
   */
  private final class Live private(cache: Cache[Key, Throwable, Option[Table]], storage: Storage) extends Database :

    import Live.*

    /* Load a table from this database. */
    override def load(key: Key): Task[Option[Table]] =
      cache get key

    /* Save a table to this database. */
    override def save(key: Key, table: Table): Task[Unit] = for
      _ <- saveToStorage(storage, key, table)
      _ <- cache invalidate key
    yield ()

    /* Delete a table from this database. */
    override def delete(key: Key): Task[Unit] = for
      _ <- deleteFromStorage(storage, key)
      _ <- cache invalidate key
    yield ()

  /**
   * Definitions associated with live database services.
   */
  private object Live:

    /** The start name. */
    private[this] val Start = "start"

    /** The storage suffix used by live database implementations. */
    private[this] val Suffix: String = ".json"

    /** The text ID. */
    private[this] val TextId = id"text"

    /** The name ID. */
    private[this] val NameId = id"name"

    /** The missing part of speech. */
    private[this] val NoPartOfSpeechId = id"no-part-of-speech"

    /**
     * Creates a live database service.
     *
     * @param cacheSize       The maximum size of the memory cache.
     * @param cacheExpiration The maximum TTL of memory cache entries.
     * @param storage         The storage service to use.
     * @return A live database service.
     */
    def apply(cacheSize: Int, cacheExpiration: FiniteDuration, storage: Storage): UIO[Live] =
      Cache.make(
        cacheSize,
        Duration ofMillis cacheExpiration.toMillis,
        Lookup(loadFromStorage(storage, _))
      ).map(new Live(_, storage))

    /**
     * Loads the table with the specified key from the supplied storage.
     *
     * @param storage The storage to load the table from.
     * @param key     The key of the table to load.
     * @return The table with the specified key loaded from the supplied storage.
     */
    private[this] def loadFromStorage(storage: Storage, key: Key) = for
      location <- locate(key)
      data <- storage.load(location)
      table <- data.fold(none)(json.parse[Table](_) map (Some(_)))
    yield table

    /**
     * Saves a table with the specified key to the supplied storage.
     *
     * @param storage The storage to save the table to.
     * @param key     The key of the table to save.
     * @param table   The table to save.
     * @return The result of saving a table with the specified key to the supplied storage.
     */
    private def saveToStorage(storage: Storage, key: Key, table: Table) = for
      location <- locate(key)
      data <- json.emit(table)
      _ <- storage.save(location, data)
    yield ()

    /**
     * Deletes a table with the specified key from the supplied storage.
     *
     * @param storage The storage to delete the table from.
     * @param key     The key of the table to delete.
     * @return The result of attempting to delete a table with the specified key from the supplied storage.
     */
    private def deleteFromStorage(storage: Storage, key: Key) =
      locate(key) flatMap storage.delete

    /**
     * Locates the specified key.
     *
     * @param key The key to locate.
     * @return The location of the specified key.
     */
    private[this] def locate(key: Key) = key match
      case Key.Start =>
        for
          start <- ID.withString(Start)
          fileId <- ID.withString(start.value + Suffix)
        yield Prefix :+ fileId
      case Key.From(Token.Text(string, partOfSpeech)) =>
        for
          fileId <- urlEncode(string) map (_ + Suffix) flatMap ID.withString
          directoryId <- partOfSpeech.fold(pure(NoPartOfSpeechId))(urlEncode(_) flatMap ID.withString)
        yield Prefix :+ TextId :+ directoryId :+ fileId
      case Key.From(Token.Name(text, category)) =>
        for
          fileId <- urlEncode(text) map (_ + Suffix) flatMap ID.withString
          directoryId <- urlEncode(category.toString) flatMap ID.withString
        yield Prefix :+ NameId :+ directoryId :+ fileId

    /**
     * Encodes an object as it would appear in a URL.
     *
     * @param toEncode The object to encode.
     * @return An object as it would appear in a URL.
     */
    inline private[this] def urlEncode(toEncode: Any) =
      Task(URLEncoder.encode(toEncode.toString, UTF_8))
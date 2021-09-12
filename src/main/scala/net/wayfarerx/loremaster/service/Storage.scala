/* Storage.scala
 *
 * Copyright (c) 2021 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this entry except in compliance with
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

/**
 * Definition of the storage service API.
 */
trait Storage:

  /**
   * Returns the locations of the entries in this storage with the specified parent.
   *
   * @param parent The parent of the entries to return, defaults to no parent.
   * @return The locations of the entries in this storage with the specified parent.
   */
  def list(parent: Option[Location] = None): Task[Set[Location]]

  /**
   * Returns the locations of the entries in this storage with the specified parent.
   *
   * @param parent The parent of the entries to return.
   * @return The locations of the entries in this storage with thev specified parent.
   */
  inline final def list(parent: Location): Task[Set[Location]] = list(Option(parent))

  /**
   * Returns true if this storage contains an entry at the specified location.
   *
   * @param location The location of the entry to check.
   * @return True if this storage contains an entry at the specified location.
   */
  def exists(location: Location): Task[Boolean]

  /**
   * Returns the instant that the specified entry was last modified if it exists.
   *
   * @param location The location of the entry to examine.
   * @return The instant that the specified entry was last modified if it exists.
   */
  def lastModified(location: Location): Task[Option[Instant]]

  /**
   * Updates the last modified instant of the specified entry if it exists.
   *
   * @param location     The location of the entry to touch.
   * @param lastModified The instant to set as the last modified time.
   */
  def touch(location: Location, lastModified: Instant): Task[Unit]

  /**
   * Loads an entry from this storage if it exists.
   *
   * @param location The location of the entry to load.
   * @return The data loaded from the specified entry in this storage if it exists.
   */
  def load(location: Location): Task[Option[String]]

  /**
   * Saves an entry in this storage.
   *
   * @param location The location of the entry to save.
   * @param data     The data to save in the entry.
   * @return The result of attempting to save an entry in this storage.
   */
  def save(location: Location, data: String): Task[Unit]

  /**
   * Deletes an entry from this storage.
   *
   * @param location The location of the entry to delete.
   * @return The result of attempting to delete an entry from this storage.
   */
  def delete(location: Location): Task[Unit]

/**
 * Definitions associated with storage services.
 */
object Storage:

  import zio.system.System
  import zio.{Has, RIO}

  import java.net.URI

  /** The live storage layer. */
  val live: zio.RLayer[System & Has[Configuration] & Has[Log.Factory], Has[Storage]] =
    zio.ZLayer.fromEffect {
      for
        system <- RIO.service[System.Service]
        config <- RIO.service[Configuration]
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Storage].getSimpleName)
        result <- apply(system, log, config.storage, config.zeitgeist)
      yield result
    }

  /** The supported storage factories. */
  private val factories: Map[String, (System.Service, Log, URI, String) => Task[Storage]] = Map(
    Local.Scheme -> { (system, log, uri, zeitgeist) =>
      Local(system, log, Option(uri.getPath) getOrElse ".", zeitgeist)
    }
  )

  /**
   * Creates a storage instance for the specified URI.
   *
   * @param system    The system service.
   * @param log       The log service to use.
   * @param uri       The URI that points to the home of the storage space.
   * @param zeitgeist The zeitgeist that the storage will operate in.
   * @return A storage instance for the specified URI.
   */
  def apply(system: System.Service, log: Log, uri: String, zeitgeist: String): Task[Storage] =
    Task(URI(uri)) flatMap (apply(system, log, _, zeitgeist))

  /**
   * Creates a storage instance for the specified URI.
   *
   * @param system    The system service.
   * @param log       The log service to use.
   * @param uri       The URI that points to the home of the storage space.
   * @param zeitgeist The zeitgeist that the storage will operate in.
   * @return A storage instance for the specified URI.
   */
  def apply(system: System.Service, log: Log, uri: URI, zeitgeist: String): Task[Storage] =
    val scheme = Option(uri) flatMap (Option apply _.getScheme) filterNot (_.isEmpty) getOrElse Local.Scheme
    factories.get(scheme.toLowerCase).fold(fail(s"Invalid Storage scheme: $scheme.")) {
      _ (system, log, uri, if zeitgeist.isEmpty then "default" else zeitgeist.toLowerCase)
    }

  /**
   * Returns the locations of the entries in the storage with the specified parent.
   *
   * @param parent The parent of the entries to return, defaults to no parent.
   * @return The locations of the entries in the storage with the specified parent.
   */
  inline def list(parent: Option[Location] = None): RIO[Has[Storage], Set[Location]] =
    RIO.service flatMap (_.list(parent))

  /**
   * Returns the locations of the entries in the storage with the specified parent.
   *
   * @param parent The parent of the entries to return.
   * @return The locations of the entries in the storage with the specified parent.
   */
  inline def list(parent: Location): RIO[Has[Storage], Set[Location]] =
    RIO.service flatMap (_.list(parent))

  /**
   * Returns true if the storage contains an entry at the specified location.
   *
   * @param location The location of the entry to check.
   * @return True if the storage contains an entry at the specified location.
   */
  inline def exists(location: Location): RIO[Has[Storage], Boolean] =
    RIO.service flatMap (_.exists(location))

  /**
   * Returns the instant that the specified entry was last modified if it exists.
   *
   * @param location The location of the entry to examine.
   * @return The instant that the specified entry was last modified if it exists.
   */
  inline def lastModified(location: Location): RIO[Has[Storage], Option[Instant]] =
    RIO.service flatMap (_.lastModified(location))

  /**
   * Updates the last modified instant of the specified entry if it exists.
   *
   * @param location     The location of the entry to touch.
   * @param lastModified The instant to set as the last modified time.
   * @return True if the entry exists.
   */
  inline def touch(location: Location, lastModified: Instant): RIO[Has[Storage], Unit] =
    RIO.service flatMap (_.touch(location, lastModified))

  /**
   * Loads an entry from the storage if it exists.
   *
   * @param location The location of the entry to load.
   * @return The data loaded from the specified entry in the storage if it exists.
   */
  inline def load(location: Location): RIO[Has[Storage], Option[String]] =
    RIO.service flatMap (_.load(location))

  /**
   * Deletes an entry from the storage.
   *
   * @param location The location of the entry to delete.
   * @return The result of attempting to delete an entry from the storage.
   */
  inline def save(location: Location, data: String): RIO[Has[Storage], Unit] =
    RIO.service flatMap (_.save(location, data))

  /**
   * Deletes an entry from the storage.
   *
   * @param name The name of the entry to delete.
   * @return The result of attempting to delete an entry from the storage.
   */
  inline def delete(location: Location): RIO[Has[Storage], Unit] =
    RIO.service flatMap (_.delete(location))

  import java.nio.file.Path
  import scala.jdk.CollectionConverters.*

  /**
   * A storage service that uses the local filesystem.
   *
   * @param log  The log to use.
   * @param root The root path in the filesystem to target.
   */
  private case class Local private(log: Log, root: Path) extends Storage :

    import Local.*

    /* Return the locations of the entries in this storage with the specified parent. */
    override def list(parent: Option[Location]) = for
      path <- parent.fold(pure(root))(resolve(_))
      result <- for
        pathIsDirectory <- isDirectory(path)
        directory <- if !pathIsDirectory then pure(Set.empty[Location]) else for
          targetChildren <- childrenOf(path)
          children <- targetChildren.foldLeft(Task(Set.empty[Location])) { (acc, child) =>
            for _acc <- acc; _entry <- relativize(child) yield _acc + _entry
          }
        yield children
      yield directory
    yield result

    /* Return true if this storage contains an entry at the specified locations. */
    override def exists(location: Location) =
      resolve(location) flatMap (pathExists(_))

    /* Return the instant that the specified entry was last modified if it exists. */
    override def lastModified(location: Location) = for
      path <- resolve(location)
      found <- pathExists(path)
      result <- if found then getPathLastModified(path) flatMap some else none
    yield result

    /* Update the last modified instant of the specified entry if it exists. */
    override def touch(location: Location, lastModified: Instant): Task[Unit] = for
      path <- resolve(location)
      found <- pathExists(path)
      _ <- if !found then unit else setPathLastModified(path, lastModified)
    yield ()

    /* Load an entry from this storage if it exists. */
    override def load(location: Location) = for
      path <- resolve(location)
      found <- pathExists(path)
      result <- if found then readStringFromPath(path) flatMap some else none
    yield result

    /* Save an entry to this storage. */
    override def save(location: Location, data: String) = for
      path <- resolve(location)
      parentIsDirectory <- isDirectory(path.getParent)
      _ <- if parentIsDirectory then unit else createDirectories(path.getParent)
      _ <- writeStringToPath(path, data)
    yield ()

    /* Delete an entry from this storage. */
    override def delete(location: Location) = for
      path <- resolve(location)
      directory <- isDirectory(path)
      _ <- if !directory then deletePath(path) else for
        children <- childrenOf(path)
        _ <- if children.isEmpty then deletePath(path) else
          fail(s"Cannot delete non-empty directory from Storage: ${location.encoded}/.")
      yield ()
    yield ()

    /**
     * Resolves a storage path from a location.
     *
     * @param location The location to resolve a storage path from.
     * @return A storage path resolved from a location.
     */
    inline private def resolve(location: Location): Task[Path] =
      Task(root.resolve(Path.of(location.head.encoded, location.tail.map(_.encoded): _*)))

    /**
     * Reletivizes a location from a storage path.
     *
     * @param path The storage path to reletivize a location from.
     * @return A location reletivized from a storage path.
     */
    inline private def relativize(path: Path): Task[Location] = for
      relativized <- Task(root.relativize(path))
      result <- Location(relativized.toString)
    yield result

  /**
   * Factory for local storage services.
   */
  private object Local:

    import java.nio.charset.StandardCharsets.UTF_8
    import java.nio.file.Files
    import java.nio.file.attribute.FileTime

    /** The URI scheme that designates the local implementation. */
    val Scheme = "file"

    /**
     * Creates a local storage from a zeitgeist and home path.
     *
     * @param root      The root path of the local storage.
     * @param log       The log to use.
     * @param zeitgeist The zeitgeist that the storage will operate in.
     * @return A local storage from a zeitgeist and home path.
     */
    def apply(system: System.Service, log: Log, root: String, zeitgeist: String): Task[Local] = for
      path <- pathOf(s"$root/$zeitgeist/")
      normalized <- {
        if "~" != path.getName(0).toString then pure(path) else {
          for
            user <- system.property("user.home") map (_ filterNot (_.isEmpty) getOrElse ".")
            home <- pathOf(user)
            normal <- Task(home resolve path.subpath(1, path.getNameCount))
          yield normal
        }
      }
      result <- toAbsolutePath(normalized)
    yield Local(log, result)

    /** Creates a path from a string. */
    inline private def pathOf(str: String) =
      Task(Path.of(str))

    /** Converts a path into its absolute form. */
    inline private def toAbsolutePath(path: Path) =
      Task(path.toAbsolutePath)

    /** Checks if a path exists. */
    inline private def pathExists(path: Path) =
      Task(Files.exists(path))

    /** Returns the instant that the specified path was last modififed. */
    inline private def getPathLastModified(path: Path) =
      Task(Files.getLastModifiedTime(path)) map (_.toInstant)

    inline private def setPathLastModified(path: Path, lastModified: Instant) =
      Task(Files.setLastModifiedTime(path, FileTime from lastModified))

    /** Checks if a path is a directory. */
    inline private def isDirectory(path: Path) =
      Task(Files.isDirectory(path))

    /** Lists the children of a path. */
    inline private def childrenOf(path: Path) =
      Task(Files.list(path).iterator.asScala)

    /** Creates directories if they do not exist. */
    inline private def createDirectories(path: Path) =
      Task(Files.createDirectories(path))

    /** Reads a string from a path. */
    inline private def readStringFromPath(path: Path) =
      Task(Files.readString(path, UTF_8))

    /** Writes a string to a path. */
    inline private def writeStringToPath(path: Path, data: String) =
      Task(Files.writeString(path, data, UTF_8))

    /** Deletes the content of a path. */
    inline private def deletePath(path: Path) =
      Task(Files.delete(path))
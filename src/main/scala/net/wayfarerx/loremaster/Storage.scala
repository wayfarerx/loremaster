/* Storage.scala
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
 * Definition of the storage service API.
 */
trait Storage:

  /**
   * Returns true if this storage contains a file with the specified name.
   *
   * @param name The name of the file to look for.
   * @return The result of attempting to return true if this storage contains a file with the specified name.
   */
  def exists(name: String): Task[Boolean]

  /**
   * Returns the instant that the specified file was last modified if it exists.
   *
   * @param name The name of the file to look for.
   * @return The result of attempting to return the instant that the specified file was last modified if it exists.
   */
  def lastModified(name: String): Task[Option[Instant]]

  /**
   * Loads a file from this storage if it exists.
   *
   * @param name The name of the file to load.
   * @return The result of attempting to load a file from this storage.
   */
  def load(name: String): Task[Option[String]]

  /**
   * Saves a file in this storage.
   *
   * @param name The name of the file to save.
   * @param data The data to save in the file.
   * @return The result of attempting to save a file in this storage.
   */
  def save(name: String, data: String): Task[Unit]

  /**
   * Deletes a file from this storage.
   *
   * @param name The name of the file to delete.
   * @return The result of attempting to delete a file from this storage.
   */
  def delete(name: String): Task[Unit]

  /**
   * Returns the names of the files in this storage with the specified parent name.
   *
   * @param name The parent name of the names to return, defaults to no parent name.
   * @return The names of the files in this storage with the specified parent name.
   */
  def list(name: String = ""): Task[Set[String]]

/**
 * Definitions associated with storage services.
 */
object Storage:

  import java.net.URI
  import zio.{Has, RIO, RLayer, ZLayer}

  /** The live storage layer. */
  val live: RLayer[Has[Configuration], Has[Storage]] =
    ZLayer.fromEffect(RIO.service flatMap (config => apply(config.storage)))

  /**
   * Creates a storage instance for the specified URI.
   *
   * @param uri The URL that points to the root of the storage space.
   * @return A storage instance for the specified URI.
   */
  def apply(uri: String): Task[Storage] = Task(URI(uri)) flatMap apply

  /**
   * Creates a storage instance for the specified URI.
   *
   * @param uri The URL that points to the root of the storage space.
   * @return A storage instance for the specified URI.
   */
  def apply(uri: URI): Task[Storage] = Option(uri.getScheme) match
    case Some(invalid) if invalid.nonEmpty && !invalid.equalsIgnoreCase("file") =>
      fail(s"""Invalid storage scheme: "$invalid".""")
    case _ =>
      Local(Option(uri.getPath) getOrElse "")

  /**
   * Returns true if the storage contains a file with the specified name.
   *
   * @param name The name of the file to look for.
   * @return The result of attempting to return true if the storage contains a file with the specified name.
   */
  inline def exists(name: String): RIO[Has[Storage], Boolean] = RIO.service flatMap (_.exists(name))

  /**
   * Returns the instant that the specified file was last modified if it exists.
   *
   * @param name The name of the file to look for.
   * @return The result of attempting to return the instant that the specified file was last modified if it exists.
   */
  inline def lastModified(name: String): RIO[Has[Storage], Option[Instant]] = RIO.service flatMap (_.lastModified(name))

  /**
   * Loads a file from the storage.
   *
   * @param name The name of the file to load.
   * @return The result of attempting to load a file from the storage.
   */
  inline def load(name: String): RIO[Has[Storage], Option[String]] = RIO.service flatMap (_.load(name))

  /**
   * Saves a file to the storage.
   *
   * @param name The name of the file to save.
   * @param data The data to save to the storage.
   * @return The result of attempting to save a file to the storage.
   */
  inline def save(name: String, data: String): RIO[Has[Storage], Unit] = RIO.service flatMap (_.save(name, data))

  /**
   * Deletes a file from the storage.
   *
   * @param name The name of the file to delete.
   * @return The result of attempting to delete a file from the storage.
   */
  inline def delete(name: String): RIO[Has[Storage], Unit] = RIO.service flatMap (_.delete(name))

  /**
   * Returns the names of the entries in the storage with the specified parent name.
   *
   * @param name The parent name of the names to return, defaults to no parent name.
   * @return The names of the entries in the storage with the specified parent name.
   */
  inline def list(name: String = ""): RIO[Has[Storage], Set[String]] = RIO.service flatMap (_.list(name))

  import java.nio.file.Path
  import scala.jdk.CollectionConverters.given

  /**
   * A storage service that uses the local filesystem.
   *
   * @param root The root location in the filesystem to use.
   */
  private case class Local private(root: Path) extends Storage :

    import zio.UIO
    import Local._

    /* Return true if this storage contains a file with the specified name. */
    override def exists(name: String): Task[Boolean] = for
      path <- decode(name)
      result <- filesExists(path)
    yield result

    /* Return the instant that the specified file was last modified if it exists. */
    override def lastModified(name: String): Task[Option[Instant]] = for
      path <- decode(name)
      exists <- filesExists(path)
      result <- if !exists then UIO.none else for
        directory <- filesIsDirectory(path)
        instant <- if directory then UIO.none else filesGetLastModified(path) map (Some(_))
      yield instant
    yield result

    /* Load a file from this storage if it exists. */
    override def load(name: String) = for
      path <- decode(name)
      exists <- filesExists(path)
      result <- if !exists then UIO.none else for
        directory <- filesIsDirectory(path)
        data <- if directory then UIO.none else filesReadString(path) map (Some(_))
      yield data
    yield result

    /* Save a file to this storage. */
    override def save(name: String, data: String) = for
      path <- decode(name)
      parentDirectory <- filesIsDirectory(path.getParent)
      _ <- if parentDirectory then UIO.unit else filesCreateDirectories(path.getParent)
      _ <- filesWriteString(path, data)
    yield ()

    /* Delete a file from this storage. */
    override def delete(name: String) = for
      path <- decode(name)
      directory <- filesIsDirectory(path)
      _ <- if !directory then filesDelete(path) else for
        children <- filesList(path)
        _ <- if children.isEmpty then filesDelete(path) else for
          encoded <- encode(path, true)
          _ <- fail(s"""Cannot delete non-empty directory: "$encoded".""")
        yield ()
      yield ()
    yield ()

    /* Return the names of the entries in this storage with the specified parent name. */
    override def list(name: String) = for
      path <- decode(name)
      directory <- filesIsDirectory(path)
      result <- if !directory then UIO(Set.empty[String]) else for
        children <- filesList(path)
        set <- children.foldLeft(UIO(Set.empty[String]): Task[Set[String]]) { (previous, next) =>
          for
            accumulator <- previous
            childDirectory <- filesIsDirectory(next)
            encoded <- encode(next, childDirectory)
          yield if ListFilesIgnores(encoded) then accumulator else accumulator + encoded
        }
      yield set
    yield result

    /**
     * Encodes a path into a name that omits the root path.
     *
     * @param path      The path to format.
     * @param directory True if the path represents a directory.
     * @return The encoded path that omits the root path.
     */
    private def encode(path: Path, directory: Boolean): Task[String] = for
      relativized <- relativize(root, path)
    yield relativized.iterator.asScala.mkString("", "/", if directory then "/" else "")

    /**
     * Securely decodes a name into a path that is a child of the root path.
     *
     * @param name The name to decode.
     * @return The securely decoded name as a child of the root path.
     */
    private def decode(name: String): Task[Path] = for
      normalized <- pathOf(name.replaceFirst("^([\\/]+)", "")) map (_.normalize)
      result <-
        if normalized.getNameCount == 0 then UIO(root)
        else if normalized.getName(0).toString == ".." then fail(s"""Invalid name escapes from storage: "$name".""")
        else resolve(root, normalized)
    yield result

  /**
   * Factory for local storage systems.
   */
  private object Local:

    import java.nio.file.Files
    import java.nio.charset.StandardCharsets.UTF_8

    /** Names that are never listed. */
    private val ListFilesIgnores = Set("/", "~/")

    /**
     * Creates a local storage from a root path.
     *
     * @param root The root path of the local storage.
     * @return A local storage from a root path.
     */
    def apply(root: String): Task[Local] = for
      path <- pathOf(if root.isEmpty then "." else root)
      absolute <- toAbsolutePath(path)
      target <- filesCreateDirectories(absolute)
    yield Local(target)

    /** Creates a path from a string. */
    inline private def pathOf(str: String) = Task(Path.of(str))

    /** Checks if a path exists. */
    inline private def filesExists(path: Path) = Task(Files.exists(path))

    /** Returns the instant that the specified path was last modififed. */
    inline private def filesGetLastModified(path: Path) = Task(Files.getLastModifiedTime(path)) map (_.toInstant)

    /** Converts a path into its absolute form. */
    inline private def toAbsolutePath(path: Path) = Task(path.toAbsolutePath)

    /** Checks if a path is a directory. */
    inline private def filesIsDirectory(path: Path) = Task(Files.isDirectory(path))

    /** Resolves a child path against a parent path. */
    inline private def resolve(parent: Path, child: Path) = Task(parent.resolve(child))

    /** Relativises the descendant path against an ancestor path. */
    inline private def relativize(ancestor: Path, descendant: Path) = Task(ancestor.relativize(descendant))

    /** Lists the children of a path. */
    inline private def filesList(path: Path) = Task(Files.list(path).iterator.asScala)

    /** Creates directories if they do not exist. */
    inline private def filesCreateDirectories(path: Path) = Task(Files.createDirectories(path))

    /** Reads a string from a path. */
    inline private def filesReadString(path: Path) = Task(Files.readString(path, UTF_8))

    /** Writes a string to a path. */
    inline private def filesWriteString(path: Path, data: String) = Task(Files.writeString(path, data, UTF_8))

    /** Deletes the content of a path. */
    inline private def filesDelete(path: Path) = Task(Files.delete(path))
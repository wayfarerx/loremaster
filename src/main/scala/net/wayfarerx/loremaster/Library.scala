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
   * Returns the lore IDs that this library provides.
   *
   * @return The lore IDs that this library provides.
   */
  def list: Task[Set[String]]

  /**
   * Returns the lore with the specified ID.
   *
   * @param id The ID of the lore to return.
   * @return The lore with the specified ID.
   */
  def load(id: String): Task[Lore]

/**
 * Definitions associated with library services.
 */
object Library:

  import zio.{Has, RIO, RLayer, UIO, ZLayer}

  /** The designator for the TES Imperial Library. */
  val TesImperialLibraryDesignator = "TesImperialLibrary"

  /** The live library layer. */
  val live: RLayer[Has[Storage] & Has[Configuration], Has[Library]] =
    ZLayer fromEffect {
      for
        storage <- RIO.service[Storage]
        configuration <- RIO.service[Configuration]
        library <- apply(storage, configuration.library)
      yield library
    }

  /**
   * Creates an library of the specified designator's type.
   *
   * @param storage    The storage service to use for caching.
   * @param designator The designator that identifies the type of library to create.
   * @return Ann library of the specified designator's type.
   */
  def apply(storage: Storage, designator: String): Task[Library] = designator match
    case tesImperialLibrary if tesImperialLibrary.trim equalsIgnoreCase TesImperialLibraryDesignator =>
      UIO(TesImperialLibrary(storage))
    case invalid =>
      fail(s"""Invalid library designator: "$invalid".""")

  /**
   * Returns the lore IDs that the library provides.
   *
   * @return The lore IDs that the library provides.
   */
  inline def list: RIO[Has[Library], Set[String]] = for
    library <- RIO.service
    result <- library.list
  yield result

    /**
   * Returns the lore with the specified ID.
   *
   * @param id The ID of the lore to return.
   * @return The lore with the specified ID.
   */
  inline def load(id: String): RIO[Has[Library], Lore] = for
    library <- RIO.service
    result <- library.load(id)
  yield result

  import java.net.URI

  trait Website extends Library :

    protected val storage: Storage

    protected val designator: String

    protected val index: Task[URI]

    private lazy val cache = s"library/$designator/"

    final override val list: Task[Set[String]] = for
      indexAt <- index
      webpage <- downloadWebpage(indexAt)
      result <- parseIndex(webpage) map (_.view.mapValues(_.toString).toMap)
    yield result

    final override def load(id: String): Task[Lore] = for
      loreAt <- Task(URI(id))
      webpage <- downloadWebpage(loreAt)
      result <- parseLore(webpage)
    yield result

    protected def parseIndex(webpage: String): Task[Set[URI]]

    protected def parseLore(webpage: String): Task[Lore]

    private final lazy def pathOf(uri: URI): Task[String] = ???

    private final def downloadWebpage(at: URI): Task[String] = ???

  /**
   * The Elder Scrolls Imperial Library library.
   */
  private case class TesImperialLibrary(storage: Storage) extends Website :

    override protected val designator: String = TesImperialLibraryDesignator

    override protected val index = Task(URI("https://www.imperial-library.info/books/all/by-title/"))

    override protected def parseIndex(webpage: String): Task[Set[URI]] = ???

    override protected def parseLore(webpage: String): Task[Lore] = ???
/* Authority.scala
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

import net.wayfarerx.loremaster

import java.time.Instant
import zio.Task

/**
 * Definition of the authority service API.
 */
trait Authority:

  import Authority.Index

  /**
   * Downloads the index of IDs to locations that this authority provides.
   *
   * @param ifModifiedAfter The optional constraint on when the index was last modified.
   * @return The index of IDs to locations that this authority provides.
   */
  def downloadIndex(ifModifiedAfter: Option[Instant] = None): Task[Option[Index]]

  /**
   * Downloads the index of IDs to locations that this authority provides.
   *
   * @param ifModifiedAfter The constraint on when the index was last modified.
   * @return The index of IDs to locations that this authority provides.
   */
  inline final def downloadIndex(ifModifiedAfter: Instant): Task[Option[Index]] =
    downloadIndex(Some(ifModifiedAfter))

  /**
   * Downloads the lore at the specified location.
   *
   * @param location        The location of the lore to download.
   * @param ifModifiedAfter The optional constraint on when the lore was last modified.
   * @return The lore at the specified location.
   */
  def downloadLore(location: String, ifModifiedAfter: Option[Instant] = None): Task[Option[Lore]]

  /**
   * Downloads the lore at the specified location.
   *
   * @param location        The location of the lore to download.
   * @param ifModifiedAfter The constraint on when the lore was last modified.
   * @return The lore at the specified location.
   */
  inline final def downloadLore(location: String, ifModifiedAfter: Instant): Task[Option[Lore]] =
    downloadLore(location, Some(ifModifiedAfter))

/**
 * Definitions associated with authority services.
 */
object Authority:

  import zio.{Has, RIO, RLayer, UIO, ZLayer}

  type Index = Map[String, String]

  /** The designator for the TES Imperial Library authority. */
  val TesImperialLibrary = "TesImperialLibrary"

  /** The live library layer. */
  val live: RLayer[Has[Configuration], Has[Authority]] =
    ZLayer.fromEffect(RIO.service flatMap (config => apply(config.authority)))

  /**
   * Creates an authority of the specified designator's type.
   *
   * @param designator The designator that identifies the type of authority to create.
   * @return Ann authority of the specified designator's type.
   */
  def apply(designator: String): Task[Authority] = designator match
    case tesImperialLibrary if tesImperialLibrary.trim equalsIgnoreCase TesImperialLibrary => UIO(ImperialLibrary)
    case invalid => fail(s"""Invalid authority designator: "$invalid".""")

  /**
   * Downloads the index of IDs to locations that the authority provides.
   *
   * @param ifModifiedAfter The optional constraint on when the index was last modified.
   * @return The index of IDs to locations that the authority provides.
   */
  inline def downloadIndex(ifModifiedAfter: Option[Instant] = None): RIO[Has[Authority], Option[Index]] =
    RIO.service flatMap (_.downloadIndex(ifModifiedAfter))

  /**
   * Downloads the index of IDs to locations that the authority provides.
   *
   * @param ifModifiedAfter The constraint on when the index was last modified.
   * @return The index of IDs to locations that the authority provides.
   */
  inline def downloadIndex(ifModifiedAfter: Instant): RIO[Has[Authority], Option[Index]] =
    RIO.service flatMap (_.downloadIndex(ifModifiedAfter))

  /**
   * Downloads the lore at the specified location.
   *
   * @param location        The location of the lore to download.
   * @param ifModifiedAfter The optional constraint on when the lore was last modified.
   * @return The lore at the specified location.
   */
  inline def downloadLore(location: String, ifModifiedAfter: Option[Instant] = None): RIO[Has[Authority], Option[Lore]] =
    RIO.service flatMap (_.downloadLore(location, ifModifiedAfter))

  /**
   * Downloads the lore at the specified location.
   *
   * @param location        The location of the lore to download.
   * @param ifModifiedAfter The constraint on when the lore was last modified.
   * @return The lore at the specified location.
   */
  inline def downloadLore(location: String, ifModifiedAfter: Instant): RIO[Has[Authority], Option[Lore]] =
    RIO.service flatMap (_.downloadLore(location, ifModifiedAfter))

  import java.net.URL

  trait Website extends Authority:

    val indexAt: Task[URL]

    final override def downloadIndex(ifModifiedAfter: Option[Instant]): Task[Option[Index]] = for
      index <- indexAt
      webpage <- downloadWebpage(index, ifModifiedAfter)
      result <- webpage map (parseIndex(_) map (Some(_))) getOrElse UIO.none
    yield result

    final override def downloadLore(location: String, ifModifiedAfter: Option[Instant]): Task[Option[Lore]] = ???

    protected def parseIndex(webpage: String): Task[Index]

    private final def downloadWebpage(at: URL, ifModifiedAfter: Option[Instant]): Task[Option[String]] = ???

  /**
   * The Elder Scrolls Imperial Library authority.
   */
  private object ImperialLibrary extends Website :

    override val indexAt = Task(URL("https://www.imperial-library.info/books/all/by-title"))

    override protected def parseIndex(webpage: String): Task[Index] = ???
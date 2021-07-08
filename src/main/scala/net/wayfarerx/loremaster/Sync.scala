/* Sync.scala
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

import zio.Task

/**
 * Definition of the sync service API.
 */
trait Sync:

  import Sync.Frequency

  /**
   * Synchronizes the library with the authority.
   *
   * @param frequency The frequency to sync authoritative resources.
   * @return An effect that synchronizes the library with the authority.
   */
  def sync: Task[Unit]

/**
 * Definitions associated with sync services.
 */
object Sync:

  import zio.{Has, RIO, RLayer, ZLayer}

  type Requires = Has[Storage] & Has[Library] & Has[Authority]

  /** The live library layer. */
  //  val live: RLayer[Has[Storage] & Has[Library] & Has[Authority], Has[Sync]] =
  //    ZLayer.fromEffect(RIO.service flatMap apply)

  /**
   * Synchronizes the library with the authority.
   *
   * @return An effect that synchronizes the library with the authority.
   */
  inline def sync: RIO[Has[Sync], Unit] = RIO.service flatMap (_.sync)

  enum Frequency:
    case Hourly
    case Daily
    case Weekly

  def apply(configuration: Configuration): RIO[Requires, Sync] = apply(configuration.frequency)

  def apply(frequency: String): RIO[Requires, Sync] = ???

  def apply(frequency: Frequency): RIO[Requires, Sync] = ???

  /**
   * A sync service over the specified storage, library and authority services.
   *
   * @param frequency The frequency to sync authoritative resources.
   * @param storage   The storage service to use.
   * @param library   The library service to use.
   * @param authority The authority service to use.
   */
  private case class Live(frequency: Frequency, storage: Storage, library: Library, authority: Authority) extends Sync :

    import java.time.Instant
    import io.circe.generic.auto._
    import io.circe.syntax._
    import zio.UIO
    import Live._

    /* Synchronize the library with the authority. */
    override def sync: Task[Unit] = for
      index <- syncIndex
    yield ()

    /**
     * Synchronizes the cached index with the authority's index.
     *
     * @return The synchronized index.
     */
    private def syncIndex: Task[Map[String, String]] = for
      lastModified <- storage.lastModified(Location)
      downloaded <- authority.downloadIndex(lastModified)
      index <- downloaded map { index =>
        for
          json <- Task(index.asJson.noSpaces)
          _ <- storage.save(Location, json)
        yield index
      } getOrElse {
        for
          text <- storage load Location
          result <- text match {
            case Some(json) =>
              for
                decoded <- Task(io.circe.parser.decode[Map[String, String]](json))
                result <- decoded.fold(fail(_), UIO(_))
              yield result
            case None => UIO(Map.empty[String, String])
          }
        yield result
      }
    yield index

  private object Live extends ((Frequency, Storage, Library, Authority) => Live) :

    private val Location = "sync/index.json"
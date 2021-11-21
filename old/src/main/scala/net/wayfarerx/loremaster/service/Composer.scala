/* Composer.scala
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

import zio.{Has, RIO, Task}
import zio.random.Random
import model.*

/**
 * Definition of the composer service API.
 */
trait Composer:

  /**
   * Composes a story from the specified template.
   *
   * @param template The template that defines the shape of the story.
   * @return A story from the specified template.
   */
  def compose(template: Composer.Template): Task[String]

/**
 * Definitions associated with composer services.
 */
object Composer:

  val live: zio.RLayer[Has[Log.Factory] & Random & Has[Database], Has[Composer]] =
    zio.ZLayer fromEffect {
      for
        logFactory <- RIO.service[Log.Factory]
        log <- logFactory(classOf[Composer].getSimpleName)
        rng <- RIO.service[Random.Service]
        database <- RIO.service[Database]
      yield Live(log, rng, database)
    }

  /**
   * Creates a composer service bound to the specified database.
   *
   * @param log      The log to append to.
   * @param rng      The random number generator to use.
   * @param database The database to use.
   */
  def apply(log: Log, rng: Random.Service, database: Database): Composer =
    Live(log, rng, database)

  /**
   * Composes a story from the specified template.
   *
   * @param template The template that defines the shape of the story.
   * @return A story from the specified template.
   */
  inline def compose(template: Template): RIO[Has[Composer], String] =
    RIO.service flatMap (_.compose(template))

  /**
   * A template that decribes the content to compose.
   *
   * @param paragraphs The descriptions of the paragraphs to create.
   * @param characterLimit The maximum number of characters to emit.
   */
  case class Template(paragraphs: List[Template.Paragraph], characterLimit: Int)

  /**
   * Definitions associate with the template implementations.
   */
  object Template extends ((List[Template.Paragraph], Int) => Template):

    /**
     * A template of a single paragraph.
     *
     * @param sentenceCount The number of sentences to include.
     */
    case class Paragraph(sentenceCount: Int)

  /**
   * The live composer implementation.
   *
   * @param log      The log to append to.
   * @param rng      The random number generator to use.
   * @param database The database to use.
   */
  private case class Live(log: Log, rng: Random.Service, database: Database) extends Composer :

    /* Compose a story from the specified template. */
    override def compose(template: Template): Task[String] =
      ???

/* Editor.scala
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

import cats.data.NonEmptyMap

import zio.{Has, RIO, RLayer, Task, ZLayer}

import model.*

/**
 * Definition of the editor service API.
 */
trait Editor:

  /**
   * Performs edits targeting the existing lore if it differs from the replacement lore.
   *
   * @param libraryId       The library ID of the existing lore to load.
   * @param replacementLore The optional lore to replace the existing lore with.
   * @return The result of performing edits to the lore.
   */
  def edit(libraryId: ID, replacementLore: Option[Lore]): Task[Unit]

/**
 * Definitions associated with editor services.
 */
object Editor:

  /** The live editor layer. */
  val live: RLayer[Has[Library] & Has[Database], Has[Editor]] =
    ZLayer fromEffect {
      for
        library <- RIO.service[Library]
        database <- RIO.service[Database]
      yield apply(library, database)
    }

  /**
   * Creates a live editor service.
   *
   * @param library  The library service to use.
   * @param database The database service to use.
   * @return A live editor service.
   */
  def apply(library: Library, database: Database): Editor = Live(library, database)

  /**
   * Performs edits targeting the existing lore if it differs from the replacement lore.
   *
   * @param libraryId       The ID of the optional existing lore to load.
   * @param replacementLore The optional lore to replace the existing lore with.
   * @return The existing lore and the resulting lore after edits have been performed.
   */
  inline def edit(libraryId: ID, replacementLore: Option[Lore]): RIO[Has[Editor], Unit] =
    RIO.service flatMap (_.edit(libraryId, replacementLore))

  /**
   * The live editor implementation.
   *
   * @param library  The library service to use.
   * @param database The database service to use.
   * @return A live editor service.
   */
  private final class Live(library: Library, database: Database) extends Editor :

    import Live.*

    /* Perform edits targeting the existing lore if it differs from the replacement lore. */
    override def edit(libraryId: ID, replacementLore: Option[Lore]): Task[Unit] = for
      existing <- library load libraryId
      _ <- if existing == replacementLore then existing.fold(unit)(_ => library touch libraryId) else
        for
          _ <- existing.fold(unit) { lore =>
            editParagraphs(lore.toList, s => database.connection use (editing(_, s, exists = false)))
          }
          _ <- replacementLore.fold(library delete libraryId) { lore =>
            for
              _ <- editParagraphs(lore.toList, s => database.connection use (editing(_, s, exists = true)))
              _ <- library.save(libraryId, lore)
            yield ()
          }
        yield ()
    yield ()

  /**
   * Definitions associated with live editor services.
   */
  private object Live:

    /** Side effects performed on a sentence. */
    private type Edits = Sentence => Task[Unit]

    /**
     * Perform the specified edits on the supplied paragraphs.
     *
     * @param paragraphs The paragraphs to edit.
     * @param edits      The edits to perform.
     * @return The result of performing the specified edits on the supplied paragraphs.
     */
    private def editParagraphs(paragraphs: List[Paragraph], edits: Edits): Task[Unit] = paragraphs match
      case head :: tail => editSentences(head.toList, edits) flatMap (_ => editParagraphs(tail, edits))
      case Nil => unit

    /**
     * Perform the specified edits on the supplied sentences.
     *
     * @param sentences The sentences to edit.
     * @param edits     The edits to perform.
     * @return The result of performing the specified edits on the supplied sentences.
     */
    private[this] def editSentences(sentences: List[Sentence], edits: Edits): Task[Unit] = sentences match
      case head :: tail => edits(head) flatMap (_ => editSentences(tail, edits))
      case Nil => unit

    /**
     * The process for editing a database connection with a single sentence.
     *
     * @param connection The database connection to edit.
     * @param sentence   The sentence to edit into the database connection.
     * @param exists     True if the sentence should be added to the database connection, otherwise it is removed.
     * @return The result of editing a database connection with a single sentence.
     */
    private def editing(connection: Database.Connection, sentence: Sentence, exists: Boolean): Task[Unit] =

      def continue(head: Key, tail: List[Token]): Task[Unit] = for
        loaded <- connection.load(head)
        _ <- {
          val target = tail.headOption.fold(Target.End)(Target.Continue.apply)
          if exists then Some(increment(loaded, target)) else loaded.flatMap(decrement(_, target))
        }.fold(connection.delete(head))(connection.save(head, _))
        _ <- tail match
          case next :: remaining => continue(Key.From(next), remaining)
          case Nil => unit
      yield ()

      continue(Key.Start, sentence.toList)

    /**
     * Increments an entry in a table.
     *
     * @param table  The optional table to increment an entry in.
     * @param target The target to increment the entry of.
     * @return A table with the specified entry incremented.
     */
    inline private[this] def increment(table: Option[Table], target: Target): Table =
      table.fold(Table.of(target -> 1L))(_table => _table ++ Table.of(target -> _table(target).fold(1L)(_ + 1L)))

    /**
     * Decrements an entry in a table.
     *
     * @param table  The table to decrement an entry in.
     * @param target The target to decrement the entry of.
     * @return An optional table with the specified entry decremented.
     */
    inline private[this] def decrement(table: Table, target: Target): Option[Table] =
      table(target).fold(Some(table)) { count =>
        if count <= 1L then NonEmptyMap fromMap table - target else Some(table ++ Table.of(target -> (count - 1L)))
      }
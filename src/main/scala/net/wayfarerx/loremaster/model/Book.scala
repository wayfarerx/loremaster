/* Book.scala
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
package model

/**
 * A single lore book.
 *
 * @param title   The optional title of this book.
 * @param author  The optional author of this book.
 * @param content The paragraphs that comprise the content of this book.
 */
case class Book(title: Option[String], author: Option[String], content: cats.data.NonEmptyList[String])
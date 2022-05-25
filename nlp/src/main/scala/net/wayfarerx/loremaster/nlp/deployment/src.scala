/* src.scala
 *
 * Copyright (c) 2022 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package nlp
package deployment

import java.io.{FileNotFoundException, InputStream}
import java.net.URI

import zio.{IO, Task, TaskManaged, UIO, ZManaged}

/** A string containing a single space character. */
val Space = " "

/** The name of the latin detokenizer XML resource. */
val DefaultDetokenizerDictionary = "opennlp/latin-detokenizer.xml"

/**
 * Loads data from the specified URI or the fallback classpath resource.
 *
 * @param uri              The optional URI to load from.
 * @param fallbackResource The fallback classpath resource.
 * @return Data from the specified URI or the fallback classpath resource.
 */
private def load(uri: Option[URI], fallbackResource: String): TaskManaged[InputStream] = ZManaged.fromAutoCloseable {
  for
    specified <- uri.fold(Task.none)(Task some _.toURL)
    selected <- specified.fold(Task(Option(getClass.getClassLoader.getResource(fallbackResource))))(Task.some)
    result <- selected.fold {
      Task.fail(FileNotFoundException(uri.fold(fallbackResource)(_.toString)))
    }(Task apply _.openStream)
  yield result
}
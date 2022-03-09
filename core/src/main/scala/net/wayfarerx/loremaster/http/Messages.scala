/* Messages.scala
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
package http

import java.io.IOException
import java.net.URI

/**
 * The messages provided by the HTTP support package.
 */
private object Messages:

  def transportFailure(method: String, uri: URI, thrown: IOException): String =
    s"HTTP transport failure: $method $uri => ${describe(thrown)}"

  def problematicResponse(method: String, uri: URI, statusCode: Int): String =
    s"Problematic HTTP response: $method $uri => $statusCode"

  def unexpectedFailure[T: Http.Resource](resource: T, thrown: Throwable): String =
    s"Unexpected HTTP failure: $resource => ${describe(thrown)}"
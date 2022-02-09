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
package model

/**
 * The messages provided by the loremaster model.
 */
private object Messages:

  inline def invalidId(repr: String): String =
    s"Unable to decode ID from $repr"

  inline def invalidLocation(repr: String): String =
    s"Unable to decode location from $repr"

  inline def invalidToken: String =
    "Unable to decode token from JSON"

  inline def invalidNameTokenCategory(repr: String): String =
    s"Unable to decode name token category from $repr"

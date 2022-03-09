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
package aws

/**
 * The messages provided by the AWS package.
 */
private object Messages {

  def okay: String = "200 OK"

  def beforeHandlingSqsInput: String = "Before handling SQS input"

  def afterHandlingSqsInput: String = "After handling SQS input"

  def failedToDecodeSqsMessage(message: String): String = s"Failed to decode SQS message: $message"

  def failedToHandleSqsMessage(message: String): String = s"Failed to handle SQS message: $message"

  def securityException(thrown: SecurityException): String = s"Encountered security exception: ${thrown.getMessage}"

}

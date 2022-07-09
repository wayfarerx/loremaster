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
 * The messages provided by the deploy package.
 */
private object Messages {

  def okay: String = "200 OK"

  def version: String = s"The version of $Application to use."

  def memorySizeInMB(functionName: String): String =
    s"The amount of memory afforded to the $Application $functionName function in MB."

  def timeoutInSeconds(functionName: String): String =
    s"The timeout enforced on the $Application $functionName function in seconds."

  def enabled(functionName: String): String =
    s"The $Application $functionName function enablement state."

  def batchSize(functionName: String): String =
    s"The $Application $functionName function batch size."

  def maximumBatchingWindowInSeconds(functionName: String): String =
    s"The $Application $functionName function maximum batching window in seconds."

  def unableToAccessConfigurationData(key: String): String =
    s"""Unable to access configuration data for "$key" in the environment."""

  def invalidConfigurationData(key: String, `type`: String): String =
    s"""Invalid configuration data for "$key: ${`type`}" found in the environment."""

  def invalidLoggingConfiguration(name: String): String =
    s"""Invalid logging configuration for "$name"."""

  def failedToDeliverSqsMessage(message: String): String =
    s"Failed to deliver SQS message: $message."

  def failedToParseSqsMessage(message: String): String =
    s"Failed to parse SQS message: $message."

  def failedToSendSqsMessage(message: String): String =
    s"Failed to send SQS message: $message"

}

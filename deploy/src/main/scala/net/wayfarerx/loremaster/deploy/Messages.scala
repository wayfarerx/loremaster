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
package deploy

/**
 * The messages provided by the deploy package.
 */
private object Messages {

  // AWS

  def failedToDecodeSqsMessage(message: String): String = s"Failed to decode SQS message: $message"

  def failedToHandleSqsMessage(message: String): String = s"Failed to handle SQS message: $message"

  def securityException(thrown: SecurityException): String = s"Encountered security exception: ${thrown.getMessage}"

  // Strings

  def okay: String = "200 OK"

  def description: String = s"The $Application deployment stack."

  def awsAccountId: String = s"The AWS Account ID to use for $Application."

  def awsS3Bucket: String = s"The AWS S3 bucket where the $Application code is stored."

  def awsS3Key: String = s"The AWS S3 key where the $Application code is stored."

  def twitterBearerToken: String = s"The $Application Twitter bearer token."

  def twitterConnectionTimeout: String = s"The $Application Twitter connection timeout."

  def twitterRetryPolicy: String = s"The $Application Twitter retry policy."

  def failedToWriteAwsCloudFormationTemplate(thrown: Throwable): String =
    s"Failed to write AWS CloudFormation template for $Application: ${thrown.getClass.getSimpleName}(${
      Option(thrown.getMessage) getOrElse ""
    })"

}

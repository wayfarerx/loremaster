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

  def description: String = s"The $Loremaster deployment stack."

  def awsAccountId: String = s"The AWS Account ID to use for $Loremaster."

  def awsS3Bucket: String = s"The AWS S3 bucket where the $Loremaster code is stored."

  def awsS3Key: String = s"The AWS S3 key where the $Loremaster code is stored."

  def twitterBearerToken: String = s"The $Loremaster Twitter bearer token."

  def twitterConnectionTimeout: String = s"The $Loremaster Twitter connection timeout."

  def twitterRetryPolicy: String = s"The $Loremaster Twitter retry policy."

  def failedToWriteAwsCloudFormationTemplate(thrown: Throwable): String =
    s"Failed to write AWS CloudFormation template for $Loremaster: ${thrown.getClass.getSimpleName}(${
      Option(thrown.getMessage) getOrElse ""
    })"

}

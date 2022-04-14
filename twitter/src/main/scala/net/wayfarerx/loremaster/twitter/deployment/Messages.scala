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
package twitter
package deployment

/**
 * The messages provided by the twitter package.
 */
private object Messages:

  def description: String = s"The function that handles $Application Twitter events."

  def s3Bucket: String = s"The S3 bucket where the $Application Twitter code is stored."

  def s3Key: String = s"The S3 key where the $Application Twitter code is stored."

  def bearerToken: String = s"The $Application Twitter bearer token."

  def memorySize: String = s"The amount of memory afforded to the $Application Twitter function."

  def timeout: String = s"The timeout applied to the $Application Twitter function."

  def connectionTimeout: String = s"The $Application Twitter connection timeout."

  def retryPolicy: String = s"The $Application Twitter retry policy."
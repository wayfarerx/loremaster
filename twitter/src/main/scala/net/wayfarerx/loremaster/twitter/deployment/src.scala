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
package twitter
package deployment

import scala.concurrent.duration.FiniteDuration

import deployments.*

/** The Twitter string. */
private inline def Twitter = "Twitter"

/** The name of the S3 bucket parameter that contains the Twitter code. */
inline def TwitterS3Bucket = s"$Twitter$S3Bucket"

/** The name of the S3 key parameter that contains the Twitter code. */
inline def TwitterS3Key = s"$Twitter$S3Key"

/** The Twitter memory size variable name. */
inline def TwitterMemorySize = s"$Twitter$MemorySize"

/** The Twitter timeout variable name. */
inline def TwitterTimeout = s"$Twitter$Timeout"

/** The Twitter queue name variable name. */
inline def TwitterQueueName: String = s"${Twitter}QueueName"

/** The Twitter bearer token variable name. */
inline def TwitterBearerToken: String = s"${Twitter}BearerToken"

/** The Twitter connection timeout variable name. */
inline def TwitterConnectionTimeout: String = s"${Twitter}ConnectionTimeout"

/** The Twitter retry policy variable name. */
inline def TwitterRetryPolicy = s"${Twitter}RetryPolicy"
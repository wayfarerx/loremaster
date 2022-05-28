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

/** The "Twitter" prefix. */
private inline def Twitter = "Twitter"

/** The Twitter memory size variable name. */
inline def TwitterMemorySizeInMB = s"$Twitter${MemorySize}InMB"

/** The Twitter timeout variable name. */
inline def TwitterTimeoutInSeconds = s"$Twitter${Timeout}InSeconds"

/** The Twitter queue name variable name. */
inline def TwitterQueueName: String = s"${Twitter}QueueName"

/** The Twitter consumer key variable name. */
inline def TwitterConsumerKey: String = s"${Twitter}ConsumerKey"

/** The Twitter consumer secret variable name. */
inline def TwitterConsumerSecret: String = s"${Twitter}ConsumerSecret"

/** The Twitter access token variable name. */
inline def TwitterAccessToken: String = s"${Twitter}AccessToken"

/** The Twitter access token secret variable name. */
inline def TwitterAccessTokenSecret: String = s"${Twitter}AccessTokenSecret"

/** The Twitter connection timeout variable name. */
inline def TwitterConnectionTimeout: String = s"${Twitter}ConnectionTimeout"

/** The Twitter retry policy variable name. */
inline def TwitterRetryPolicy = s"${Twitter}RetryPolicy"

/** The Twitter enablement variable name. */
inline def TwitterEnabled = s"${Twitter}Enabled"

/** The Twitter batch size variable name. */
inline def TwitterBatchSize = s"${Twitter}BatchSize"

/** The Twitter maximum batching window variable name. */
inline def TwitterMaximumBatchingWindowInSeconds = s"${Twitter}MaximumBatchingWindowInSeconds"
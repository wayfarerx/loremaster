/* TwitterDeployment.scala
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

import scala.collection.immutable.ListMap
import scala.language.implicitConversions

import io.circe.Json

import deployments.*
import event.*

/**
 * Deploys the Twitter components.
 */
trait TwitterDeployment extends Deployment :

  /* The parameters required by Twitter deployments. */
  override def parameters: Entries = super.parameters +
    parameter(TwitterMemorySizeInMB, Messages.memorySize, defaultFunctionMemorySizeInMB) +
    parameter(TwitterTimeoutInSeconds, Messages.timeout, defaultFunctionTimeoutInSeconds) +
    parameter(TwitterConnectionTimeout, Messages.connectionTimeout, defaultConnectionTimeout) +
    parameter(TwitterRetryPolicy, Messages.retryPolicy, defaultRetryPolicy) +
    parameter(TwitterEnabled, Messages.enabled, defaultEnabled) +
    parameter(TwitterBatchSize, Messages.batchSize, defaultBatchSize) +
    parameter(TwitterMaximumBatchingWindowInSeconds, Messages.maxBatchingWindow, defaultMaximumBatchingWindowInSeconds)

  /* The resources provided by Twitter deployments. */
  override def resources: Entries = super.resources ++
    handleSqsMessagesWithLambdaFunction[TwitterEvent, TwitterFunction](
      Messages.description,
      Twitter.toLowerCase,
      ref(TwitterMemorySizeInMB),
      ref(TwitterTimeoutInSeconds),
      environment = Map(
        TwitterConsumerKey -> resolveSecret(TwitterConsumerKey),
        TwitterConsumerSecret -> resolveSecret(TwitterConsumerSecret),
        TwitterAccessToken -> resolveSecret(TwitterAccessToken),
        TwitterAccessTokenSecret -> resolveSecret(TwitterAccessTokenSecret),
        TwitterConnectionTimeout -> ref(TwitterConnectionTimeout),
        TwitterRetryPolicy -> ref(TwitterRetryPolicy),
        TwitterQueueName -> sqsQueueName[TwitterEvent]
      ),
      ref(TwitterEnabled),
      ref(TwitterBatchSize),
      ref(TwitterMaximumBatchingWindowInSeconds),
      Domain -> Twitter
    )

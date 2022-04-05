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

import deploy.*
import event.*

/**
 * Deploys the Twitter components.
 */
trait TwitterDeployment extends Deployment :

  /* The parameters required by Twitter deployments. */
  override def parameters: ListMap[String, Json] = super.parameters +
    parameter[String](TwitterS3Bucket, Messages.s3Bucket) +
    parameter[String](TwitterS3Key, Messages.s3Key) +
    parameter[String](TwitterBearerToken, Messages.bearerToken) +
    parameter(TwitterMemorySize, Messages.memorySize, defaultFunctionMemorySize) +
    parameter(TwitterTimeout, Messages.timeout, defaultFunctionTimeout) +
    parameter(TwitterConnectionTimeout, Messages.connectionTimeout, defaultConnectionTimeout) +
    parameter(TwitterRetryPolicy, Messages.retryPolicy, Retries.Default.toString)

  /* The resources provided by Twitter deployments. */
  override def resources: ListMap[String, Json] = super.resources ++
    sqsQueueToLambdaFunction[TwitterEvent, TwitterFunction](
      Messages.description,
      ref(TwitterS3Bucket),
      ref(TwitterS3Key),
      ref(TwitterMemorySize),
      ref(TwitterTimeout),
      permissions = Seq(),
      environment = Map(
        TwitterQueueName -> sqsQueueName[TwitterEvent],
        TwitterBearerToken -> ref(TwitterBearerToken),
        TwitterConnectionTimeout -> ref(TwitterConnectionTimeout),
        TwitterRetryPolicy -> ref(TwitterRetryPolicy)
      ),
      Domain -> Twitter
    )

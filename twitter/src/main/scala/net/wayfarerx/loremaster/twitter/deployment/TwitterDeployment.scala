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

import scala.concurrent.duration.*

import aws.*
import event.*

/**
 * Deploys the Twitter components.
 */
trait TwitterDeployment extends Deployment :

  /* The parameters required by Twitter deployments. */
  override def parameters: Entries = super.parameters ++ sqsQueueDeliversToLambdaFunctionParameters(Twitter) +
    parameter(TwitterConnectionTimeout, Messages.connectionTimeout, 5.seconds) +
    parameter(TwitterRetryPolicy, Messages.retryPolicy, RetryPolicy.Default)
    

  /* The resources provided by Twitter deployments. */
  override def resources: Entries = super.resources ++
    sqsQueueDeliversToLambdaFunction[TwitterEvent, TwitterFunction](
      Messages.description,
      Twitter,
      Map(
        TwitterConsumerKey -> resolveSecret(TwitterConsumerKey),
        TwitterConsumerSecret -> resolveSecret(TwitterConsumerSecret),
        TwitterAccessToken -> resolveSecret(TwitterAccessToken),
        TwitterAccessTokenSecret -> resolveSecret(TwitterAccessTokenSecret),
        TwitterConnectionTimeout -> ref(TwitterConnectionTimeout),
        TwitterRetryPolicy -> ref(TwitterRetryPolicy)
      )
    )

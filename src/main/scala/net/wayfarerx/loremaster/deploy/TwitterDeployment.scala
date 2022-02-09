/* DeployTwitter.scala
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

import scala.collection.immutable.ListMap

import io.circe.Json

import twitter.{TwitterEvent, TwitterFunction, TwitterConfiguration}

/**
 * Deploys the Twitter components.
 */
trait TwitterDeployment extends Deployment :

  /* The parameters required by Twitter deployments. */
  override def parameters: ListMap[String, Json] = super.parameters +
    parameter(TwitterConfiguration.BearerToken, Messages.twitterBearerToken) +
    parameter(TwitterConfiguration.ConnectionTimeout, Messages.twitterConnectionTimeout) +
    parameter(TwitterConfiguration.RetryPolicy, Messages.twitterRetryPolicy)

  /* The resources provided by Twitter deployments. */
  override def resources: ListMap[String, Json] = super.resources ++
    queue[TwitterEvent] ++
    function[TwitterEvent](
      description = "The function that handles tweet events.",
      handler = classOf[TwitterFunction].getName,
      environment = Map(
        TwitterConfiguration.BearerToken -> ref(TwitterConfiguration.BearerToken),
        TwitterConfiguration.ConnectionTimeout -> ref(TwitterConfiguration.ConnectionTimeout),
        TwitterConfiguration.RetryPolicy -> ref(TwitterConfiguration.RetryPolicy),
        TwitterEvent.Topic -> ref(queueName[TwitterEvent])
      )
    ) ++
    mapping[TwitterEvent]
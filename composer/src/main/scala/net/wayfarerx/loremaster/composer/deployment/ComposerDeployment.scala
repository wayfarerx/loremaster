/* ComposerDeployment.scala
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
package composer
package deployment

import aws.*

trait ComposerDeployment extends Deployment :

  override def parameters: Entries = super.parameters +
    parameter(ComposerMemorySizeInMB, Messages.memorySize, DefaultMemorySizeInMB) +
    parameter(ComposerTimeoutInSeconds, Messages.timeout, DefaultTimeoutInSeconds) +
    parameter(ComposerRetryPolicy, Messages.retryPolicy, DefaultRetryPolicy) +
    parameter(ComposerNlpDetokenizerDictionary, Messages.composerDetokenizerDictionary, "") +
    parameter(ComposerEnabled, Messages.enabled, DefaultEnabled) +
    parameter(ComposerBatchSize, Messages.batchSize, DefaultBatchSize) +
    parameter(ComposerMaximumBatchingWindowInSeconds, Messages.maxBatchingWindow, DefaultMaximumBatchingWindowInSeconds)

  override def resources: Entries = super.resources ++
    sqsQueueDeliversToLambdaFunction[ComposerEvent, ComposerFunction](
      Messages.description,
      Composer.toLowerCase,
      ref(ComposerMemorySizeInMB),
      ref(ComposerTimeoutInSeconds),
      environment = Map(
        ComposerRetryPolicy -> ref(ComposerRetryPolicy),
        ComposerNlpDetokenizerDictionary -> ref(ComposerNlpDetokenizerDictionary)
      ),
      ref(ComposerEnabled),
      ref(ComposerBatchSize),
      ref(ComposerMaximumBatchingWindowInSeconds),
      Domain -> Composer
    )
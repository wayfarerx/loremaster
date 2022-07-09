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

import scala.language.implicitConversions
import aws.*
import event.*
import io.circe.Json

/**
 * Deploys the composer components.
 */
trait ComposerDeployment extends Deployment :

  /* The parameters required by composer deployments. */
  override def parameters: Entries = super.parameters ++
    triggerLambdaFunctionParameters(Composer) ++
    handlerLambdaFunctionParameters(Composer) +
    parameter(ComposerDetokenizerDictionary, Messages.detokenizerDictionary, "") +
    parameter(ComposerRetryPolicy, Messages.retryPolicy, RetryPolicy.Default)

  /* The resources provided by composer deployments. */
  override def resources: Entries = super.resources ++
    triggerLambdaFunction[ComposerTrigger](
      Composer,
      Messages.triggerDescription,
      Map()
    ) ++
    handlerLambdaFunction[ComposerEvent, ComposerHandler](
      Composer,
      Messages.handlerDescription,
      Map(
        ComposerDetokenizerDictionary -> ref(ComposerDetokenizerDictionary),
        ComposerRetryPolicy -> ref(ComposerRetryPolicy)
      )
    )
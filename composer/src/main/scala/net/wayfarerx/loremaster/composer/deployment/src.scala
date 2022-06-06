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
package composer
package deployment

import aws.*

/** The "Composer" prefix. */
private inline def Composer = "Composer"

/** The Twitter memory size variable name. */
inline def ComposerMemorySizeInMB = s"$Composer$MemorySizeInMB"

/** The Twitter timeout variable name. */
inline def ComposerTimeoutInSeconds = s"$Composer$TimeoutInSeconds"

/** The composer retry policy variable name. */
inline def ComposerRetryPolicy = s"${Composer}RetryPolicy"

/** The composer NLP detokenizer dictionary variable name. */
inline def ComposerNlpDetokenizerDictionary = s"${Composer}NlpDetokenizerDictionary"

/** The Twitter enablement variable name. */
inline def ComposerEnabled = s"${Composer}Enabled"

/** The Twitter batch size variable name. */
inline def ComposerBatchSize = s"${Composer}BatchSize"

/** The Twitter maximum batching window variable name. */
inline def ComposerMaximumBatchingWindowInSeconds = s"${Composer}MaximumBatchingWindowInSeconds"
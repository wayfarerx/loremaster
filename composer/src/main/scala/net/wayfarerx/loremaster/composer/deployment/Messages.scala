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
package composer
package deployment

private object Messages:

  def description: String = s"The function that handles $Application $Composer events."

  def memorySize: String = s"The amount of memory afforded to the $Application $Composer function in MB."

  def timeout: String = s"The timeout applied to the $Application $Composer function in seconds."

  def retryPolicy: String = s"The $Application $Composer retry policy."

  def composerDetokenizerDictionary: String = s"The $Application $Composer NLP detokenizer dictionary URI."

  def enabled: String = s"The $Application $Composer enablement state."

  def batchSize: String = s"The $Application $Composer batch size."

  def maxBatchingWindow: String = s"The $Application $Composer maximum batching window in seconds."
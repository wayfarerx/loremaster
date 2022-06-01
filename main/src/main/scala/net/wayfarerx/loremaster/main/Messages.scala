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
package main

/**
 * The messages provided by the main package.
 */
private object Messages {

  /** The description of this application. */
  def description: String = s"The $Application deployment stack."

  /** The usage of this application. */
  def usage: String = s"Usage: $Application [cloudformation.json]"

  /**
   * The message that describes a failure to write an AWS CloudFormation template.
   *
   * @param thrown The throwable that prevented writing an AWS CloudFormation template.
   * @return The message that describes a failure to write an AWS CloudFormation template.
   */
  def failedToWriteAwsCloudFormationTemplate(thrown: Throwable): String =
    s"Failed to write AWS CloudFormation template for $Application: ${describe(thrown)}."

}

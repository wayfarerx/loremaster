/* TriggeredFunction.scala
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
package aws

import java.util.{Map => JMap}

import scala.jdk.CollectionConverters.*

import com.amazonaws.services.lambda.runtime.RequestHandler

import zio.RIO

/**
 * Base type for triggered Lambda functions.
 */
trait TriggeredFunction extends LambdaFunction[JMap[String, String]] with RequestHandler[JMap[String, String], String] :

  /* Process a triggered event. */
  final override def apply(event: JMap[String, String]): RIO[EnvironmentWithLog, Unit] =
    onTrigger(event.asScala.toMap)

  /**
   * Handles a triggered invocation.
   *
   * @param invocation The invocation to handle.
   * @return A task that handles the specified invocation.
   */
  protected def onTrigger(invocation: Map[String, String]): RIO[EnvironmentWithLog, Unit]
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
package aws

import scala.reflect.ClassTag

import com.amazonaws.services.lambda.runtime.Context

import zio.{Has, IO, RLayer, ZEnv, ZLayer}
import zio.system.System

import configuration.*
import event.*
import logging.*

/** The type of the AWS environment. */
type AwsEnv = ZEnv & Has[Configuration] & Has[Logging]

/** Factory for AWS environments. */
object AwsEnv extends (Context => RLayer[ZEnv, AwsEnv]) :

  /**
   * Creates a new AWS environment.
   *
   * @param context The context to create the environment in.
   * @return A new AWS environment.
   */
  override def apply(context: Context): RLayer[ZEnv, AwsEnv] =
    val config: RLayer[ZEnv, Has[Configuration]] = ZLayer.fromService(AwsConfiguration(_))
    val logs: RLayer[ZEnv, Has[Logging]] = config >>> ZLayer.fromService(AwsLogging(_, context.getLogger))
    ZLayer.requires[ZEnv] ++ config ++ logs

/** AWS extensions to all events. */
extension[T: ClassTag] (* : Event[T])

/** Returns the name of the SQS queue associated with the event. */
  def sqsQueueName: String = s"$Application${summon[ClassTag[T]].getClass.getSimpleName}Queue"

/** The "BatchSize" string. */
inline def BatchSize = "BatchSize"

/** The "Code" string. */
inline def Code = "Code"

/** The "Default" string. */
inline def Default = "Default"

/** The "Description" string. */
inline def Description = "Description"

/** The "Domain" string. */
inline def Domain = "Domain"

/** The "Enabled" string. */
inline def Enabled = "Enabled"

/** The "Environment" string. */
inline def Environment = "Environment"

/** The "EventSourceArn" string. */
inline def EventSourceArn = "EventSourceArn"

/** The "FunctionName" string. */
inline def FunctionName = "FunctionName"

/** The "Handler" string. */
inline def Handler = "Handler"

/** The "InMB" string. */
inline def InMB = "InMB"

/** The "InSeconds" string. */
inline def InSeconds = "InSeconds"

/** The "Key" string. */
inline def Key = "Key"

/** The "MaximumBatchingWindow" name. */
inline def MaximumBatchingWindow: String = "MaximumBatchingWindow"

/** The "MaximumBatchingWindowInSeconds" name. */
inline def MaximumBatchingWindowInSeconds: String = s"$MaximumBatchingWindow$InSeconds"

/** The "MemorySize" name. */
inline def MemorySize: String = "MemorySize"

/** The "MemorySizeInMB" name. */
inline def MemorySizeInMB: String = s"$MemorySize$InMB"

/** The "Properties" string. */
inline def Properties = "Properties"

/** The "QueueName" string. */
inline def QueueName = "QueueName"

/** The "Role" string. */
inline def Role = "Role"

/** The "Runtime" string. */
inline def Runtime = "Runtime"

/** The "Trigger" string. */
inline def Trigger = "Trigger"

/** The "Tags" string. */
inline def Tags = "Tags"

/** The "Timeout" name. */
inline def Timeout: String = "Timeout"

/** The "TimeoutInSeconds" name. */
inline def TimeoutInSeconds: String = s"$Timeout$InSeconds"

/** The "Type" string. */
inline def Type = "Type"

/** The "Value" string. */
inline def Value = "Value"

/** The "Variables" string. */
inline def Variables = "Variables"

/** The "Version" string. */
inline def Version = "Version"

/** The "VisibilityTimeout" string. */
inline def VisibilityTimeout = "VisibilityTimeout"
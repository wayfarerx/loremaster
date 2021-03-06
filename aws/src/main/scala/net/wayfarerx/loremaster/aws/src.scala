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
def AwsEnv(context: Context): RLayer[ZEnv, AwsEnv] =
  val config: RLayer[ZEnv, Has[Configuration]] = ZLayer.fromService(AwsConfiguration(_))
  val logs: RLayer[ZEnv, Has[Logging]] = config >>> ZLayer.fromService(AwsLogging(_, context.getLogger))
  ZLayer.requires[ZEnv] ++ config ++ logs

/** AWS extensions to all events. */
extension [T : ClassTag] (event: Event[T])

  /** Returns the name of the SQS queue associated with the event. */
  def sqsQueueName: String = s"$Application${summon[ClassTag[T]].getClass.getSimpleName}Queue"

/** The "2012-10-17" string. */
inline def _2012_10_17 = "2012-10-17"

/** The "Action" string. */
inline def Action = "Action"

/** The "Allow" string. */
inline def Allow = "Allow"

/** The "Arn" string. */
inline def Arn = "Arn"

/** The "AssumeRolePolicyDocument" string. */
inline def AssumeRolePolicyDocument = "AssumeRolePolicyDocument"

/** The "AWS" string. */
inline def AWS = "AWS"

/** The "BatchSize" string. */
inline def BatchSize = "BatchSize"

/** The "Code" string. */
inline def Code = "Code"

/** The "Default" string. */
inline def Default = "Default"

/** The "DependsOn" string. */
inline def DependsOn = "DependsOn"

/** The "Description" string. */
inline def Description = "Description"

/** The "Domain" string. */
inline def Domain = "Domain"

/** The "Effect" string. */
inline def Effect = "Effect"

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

/** The "Key" string. */
inline def Key = "Key"

/** The "Lambda" string. */
inline def Lambda = "Lambda"

/** The "MaximumBatchingWindowInSeconds" name. */
inline def MaximumBatchingWindowInSeconds: String = "MaximumBatchingWindowInSeconds"

/** The "MemorySize" name. */
inline def MemorySize: String = "MemorySize"

/** The "MemorySizeInMB" name. */
inline def MemorySizeInMB: String = s"${MemorySize}InMB"

/** The "Number" string. */
inline def Number = "Number"

/** The "Policy" string. */
inline def Policy = "Policy"

/** The "Policies" string. */
inline def Policies = "Policies"

/** The "PolicyDocument" string. */
inline def PolicyDocument = "PolicyDocument"

/** The "PolicyName" string. */
inline def PolicyName = "PolicyName"

/** The "Principal" string. */
inline def Principal = "Principal"

/** The "Properties" string. */
inline def Properties = "Properties"

/** The "QueueName" string. */
inline def QueueName = "QueueName"

/** The "Resource" string. */
inline def Resource = "Resource"

/** The "Role" string. */
inline def Role = "Role"

/** The "RoleName" string. */
inline def RoleName = "RoleName"

/** The "Runtime" string. */
inline def Runtime = "Runtime"

/** The "S3Bucket" name. */
inline def S3Bucket: String = "S3Bucket"

/** The "S3Key" name. */
inline def S3Key: String = "S3Key"

/** The "Service" string. */
inline def Service = "Service"

/** The "SQS" string. */
inline def SQS = "SQS"

/** The "Statement" string. */
inline def Statement = "Statement"

/** The "Tags" string. */
inline def Tags = "Tags"

/** The "Timeout" name. */
inline def Timeout: String = "Timeout"

/** The "TimeoutInSeconds" name. */
inline def TimeoutInSeconds: String = "TimeoutInSeconds"

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
/* Deployment.scala
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

import io.circe.Json
import io.circe.Json.{arr, obj}

import scala.collection.immutable.ListMap
import scala.language.implicitConversions
import scala.reflect.ClassTag

/**
 * Base type for participants in the deployment process.
 */
trait Deployment:

  import Deployment.*

  /** The given conversion of integers to JSON integers. */
  protected given Conversion[Int, Json] = Json.fromInt

  /** The given conversion of strings to JSON strings. */
  protected given Conversion[String, Json] = Json.fromString

  // Parameters

  /** The name of the AWS account ID parameter. */
  inline protected final def AccountID = "AccountID"

  /** The name of the AWS S3 bucket parameter. */
  inline protected final def S3Bucket = "S3Bucket"

  /** The name of the AWS S3 key parameter. */
  inline protected final def S3Key = "S3Key"

  // Strings

  /** The "2012-10-17" string. */
  inline protected final def _2012_10_17 = "2012-10-17"

  /** The "Action" string. */
  inline protected final def Action = "Action"

  /** The "Allow" string. */
  inline protected final def Allow = "Allow"

  /** The "Arn" string. */
  inline protected final def Arn = "Arn"

  /** The "AssumeRolePolicyDocument" string. */
  inline protected final def AssumeRolePolicyDocument = "AssumeRolePolicyDocument"

  /** The "AWS" string. */
  inline protected final def AWS = "AWS"

  /** The "Code" string. */
  inline protected final def Code = "Code"

  /** The "DependsOn" string. */
  inline protected final def DependsOn = "DependsOn"

  /** The "Description" string. */
  inline protected final def Description = "Description"

  /** The "Effect" string. */
  inline protected final def Effect = "Effect"

  /** The "Enabled" string. */
  inline protected final def Enabled = "Enabled"

  /** The "Environment" string. */
  inline protected final def Environment = "Environment"

  /** The "EventSourceArn" string. */
  inline protected final def EventSourceArn = "EventSourceArn"

  /** The "FunctionName" string. */
  inline protected final def FunctionName = "FunctionName"

  /** The "Handler" string. */
  inline protected final def Handler = "Handler"

  /** The "MemorySize" string. */
  inline protected final def MemorySize = "MemorySize"

  /** The "Path" string. */
  inline protected final def Path = "Path"

  /** The "Policies" string. */
  inline protected final def Policies = "Policies"

  /** The "PolicyDocument" string. */
  inline protected final def PolicyDocument = "PolicyDocument"

  /** The "PolicyDocument" string. */
  inline protected final def PolicyName = "PolicyName"

  /** The "Principal" string. */
  inline protected final def Principal = "Principal"

  /** The "Properties" string. */
  inline protected final def Properties = "Properties"

  /** The "QueueName" string. */
  inline protected final def QueueName = "QueueName"

  /** The "Queues" string. */
  inline protected final def Queues = "Queues"

  /** The "Resource" string. */
  inline protected final def Resource = "Resource"

  /** The "Role" string. */
  inline protected final def Role = "Role"

  /** The "RoleName" string. */
  inline protected final def RoleName = "RoleName"

  /** The "Runtime" string. */
  inline protected final def Runtime = "Runtime"

  /** The "Service" string. */
  inline protected final def Service = "Service"

  /** The "Statement" string. */
  inline protected final def Statement = "Statement"

  /** The "Timeout" string. */
  inline protected final def Timeout = "Timeout"

  /** The "Type" string. */
  inline protected final def Type = "Type"

  /** The "Variables" string. */
  inline protected final def Variables = "Variables"

  /** The "Version" string. */
  inline protected final def Version = "Version"

  // Implementation

  /** Generates the parameters required by this deployment. */
  def parameters: ListMap[String, Json] = ListMap(
    parameter(AccountID, Messages.awsAccountId),
    parameter(S3Bucket, Messages.awsS3Bucket),
    parameter(S3Key, Messages.awsS3Key)
  )

  /** Generates the resources provided by this deployment. */
  def resources: ListMap[String, Json] = ListMap.empty

  // Utilities

  /**
   * Generates JSON that resolves the specified attribute.
   *
   * @param target    The resource to get the attribute from.
   * @param attribute The name of the attribute to get.
   * @return JSON that resolves the specified attribute.
   */
  protected final def getAtt(target: String, attribute: String): Json =
    obj("Fn::GetAtt" -> arr(target, attribute))

  /**
   * Generates JSON that joins the supplied values using the specified delimiter.
   *
   * @param delimiter The delimiter to join values with.
   * @param values    The values to join.
   * @return JSON that joins the supplied values using the specified delimiter.
   */
  protected final def join(delimiter: String, values: Json*): Json =
    obj("Fn::Join" -> arr(delimiter, arr(values *)))

  /**
   * Generates JSON that resolves the specified reference.
   *
   * @param target The name of the reference to resolve.
   * @return JSON that resolves the specified reference.
   */
  protected def ref(target: String): Json =
    obj("Ref" -> target)

  // Factories

  /**
   * Generates a string parameter definition.
   *
   * @param name        The name of the parameter.
   * @param description The description of the parameter.
   * @return A string parameter definition.
   */
  protected final def parameter(name: String, description: String): (String, Json) =
    parameter(name, "String", description)

  /**
   * Generates a typed parameter definition.
   *
   * @param name        The name of the parameter.
   * @param _type       The type of the parameter.
   * @param description The description of the parameter.
   * @return A typed parameter definition.
   */
  protected final def parameter(name: String, _type: String, description: String): (String, Json) =
    name -> obj(Type -> _type, Description -> description)

  /**
   * Generates the name of the policy with the specified prefix.
   *
   * @param prefix The prefix to return the policy name for.
   * @return The name of the policy with the specified prefix.
   */
  protected final def policyName(prefix: String): String = s"${prefix}Policy"

  /**
   * Generates the name of the role with the specified prefix.
   *
   * @param prefix The prefix to return the role name for.
   * @return The name of the role with the specified prefix.
   */
  protected final def roleName(prefix: String): String = s"${prefix}Role"

  /**
   * Generates the name of the function associated with the specified type.
   *
   * @tparam T The type to return the name of the associated function for.
   * @return The name of the function associated with the specified type.
   */
  protected final def functionName[T: ClassTag]: String = s"${name[T]}Function"

  /**
   * Generates the name of the log group associated with the specified type.
   *
   * @tparam T The type to return the name of the associated log group for.
   * @return The name of the log group associated with the specified type.
   */
  protected final def logGroupName[T: ClassTag]: String = s"aws/lambda/${functionName[T]}"

  /**
   * Generates the name of the mapping associated with the specified type.
   *
   * @tparam T The type to return the name of the associated mapping for.
   * @return The name of the mapping associated with the specified type.
   */
  protected final def mappingName[T: ClassTag]: String = s"${name[T]}Mapping"

  /**
   * Generates the name of the queue associated with the specified type.
   *
   * @tparam T The type to return the name of the associated queue for.
   * @return The name of the queue associated with the specified type.
   */
  protected final def queueName[T: ClassTag]: String = s"${name[T]}Queue"

  /**
   * Generates a Lambda function resource definition.
   *
   * @tparam T The type of event the function handles.
   * @param description The description of the function.
   * @param handler     The name of the handler that implements the function.
   * @param memorySize  The amount of memory available to the function in MB, defaults to 128.
   * @param timeout     The amount of time the function can run in seconds, defaults to 30.
   * @param environment The environment that the function executes in.
   * @return A Lambda function resource definition.
   */
  protected final def function[T: ClassTag](
    description: String,
    handler: String,
    memorySize: Int = 1024,
    timeout: Int = 900,
    environment: Map[String, Json] = Map.empty
  ): ListMap[String, Json] =
    val _functionName = functionName[T]
    val _queueName = queueName[T]
    val _roleName = roleName(_functionName)
    ListMap(
      _roleName -> obj(
        Type -> "AWS::IAM::Role",
        Properties -> obj(
          RoleName -> _roleName,
          Path -> "/",
          Policies -> arr(obj(
            PolicyName -> policyName(_roleName),
            PolicyDocument -> obj(
              Version -> _2012_10_17,
              Statement -> arr(
                obj(
                  Effect -> Allow,
                  Action -> "s3:*",
                  Resource -> "*"
                ),
                obj(
                  Effect -> Allow,
                  Action -> arr("logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"),
                  Resource -> "*"
                ),
                obj(
                  Effect -> Allow,
                  Action -> "sqs:*",
                  Resource -> getAtt(_queueName, Arn)
                )
              )
            )
          )),
          AssumeRolePolicyDocument -> obj(
            Version -> _2012_10_17,
            Statement -> arr(obj(
              Effect -> Allow,
              Principal -> obj(Service -> arr("lambda.amazonaws.com")),
              Action -> arr("sts:AssumeRole")
            ))
          )
        )
      ),
      _functionName -> obj(
        Type -> "AWS::Lambda::Function",
        Properties -> obj(
          FunctionName -> _functionName,
          Description -> description,
          Handler -> handler,
          MemorySize -> memorySize,
          Runtime -> "java11",
          Timeout -> timeout,
          Role -> join("", "arn:aws:iam::", ref(AccountID), s":role/$_roleName"),
          Code -> obj(
            S3Bucket -> ref(S3Bucket),
            S3Key -> ref(S3Key)
          ),
          Environment -> obj(Variables -> obj(environment.toSeq *))
        ),
        DependsOn -> arr(_roleName)
      )
    )

  protected final def logGroup[T: ClassTag]: ListMap[String, Json] =
    ListMap(
      "LogGroupName" -> logGroupName[T]
    )

  /**
   * Generates an SQS to Lambda event source mapping definition.
   *
   * @tparam T The type of event the mapping manages.
   * @return An SQS to Lambda event source mapping definition.
   */
  protected final def queueFunctionMapping[T: ClassTag]: ListMap[String, Json] =
    val _queueName = queueName[T]
    val _functionName = functionName[T]
    val _mappingName = mappingName[T]
    ListMap(
      _mappingName -> obj(
        Type -> "AWS::Lambda::EventSourceMapping",
        Properties -> obj(
          Enabled -> Json.fromBoolean(true),
          EventSourceArn -> getAtt(_queueName, Arn),
          FunctionName -> ref(_functionName)
        )
      )
    )

  /**
   * Generates an SQS queue resource definition.
   *
   * @tparam T The type of event the queue manages.
   * @return An SQS queue resource definition.
   */
  protected final def queue[T: ClassTag]: ListMap[String, Json] =
    val _queueName = queueName[T]
    val _policyName = policyName(_queueName)
    ListMap(
      _queueName -> obj(
        Type -> "AWS::SQS::Queue",
        Properties -> obj(QueueName -> _queueName)
      ),
      _policyName -> obj(
        Type -> "AWS::SQS::QueuePolicy",
        Properties -> obj(
          Queues -> arr(ref(_queueName)),
          PolicyDocument -> obj(
            Statement -> arr(
              obj(
                Action -> arr("sqs:SendMessage"),
                Effect -> Allow,
                Resource -> getAtt(_queueName, Arn),
                Principal -> obj(AWS -> arr(ref(AccountID)))
              )
            )
          )
        )
      )
    )

/**
 * Definitions associated with deployments.
 */
object Deployment:

  /**
   * Returns the base name of the specified class.
   *
   * @return The base name of the specified class.
   */
  inline private[this] def name[T: ClassTag]: String =
    summon[ClassTag[T]].runtimeClass.getSimpleName
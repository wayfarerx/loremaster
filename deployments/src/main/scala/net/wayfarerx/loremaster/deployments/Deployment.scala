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
package deployments

import scala.collection.immutable.ListMap
import scala.concurrent.duration.*
import scala.language.implicitConversions
import scala.reflect.ClassTag

import io.circe.Json.{arr, fromString, obj}
import io.circe.{Encoder, Json}

import event.*

/**
 * Base type for participants in the deployment process.
 */
trait Deployment:

  /** The given conversion of integers to JSON integers. */
  protected final given Conversion[Int, Json] = Json.fromInt

  /** The given conversion of strings to JSON strings. */
  protected final given Conversion[String, Json] = Json.fromString

  // Aliases

  /** The type of individual tags. */
  protected final type Tag = Deployment.Tag

  /** Factory for individual tags. */
  protected final def Tag: Deployment.Tag.type = Deployment.Tag

  /** The type of individual deployment entries. */
  protected final type Entry = Deployment.Entry

  /** Factory for individual deployment entries. */
  protected final def Entry: Deployment.Entry.type = Deployment.Entry

  /** The type of deployment entry collections. */
  protected final type Entries = Deployment.Entries

  /** Factory for deployment entry collections. */
  protected final def Entries: Deployment.Entries.type = Deployment.Entries

  // Defaults

  /** The default memory size for Lambda functions in megabytes. */
  protected final def DefaultMemorySizeInMB: Int = 1024

  /** The default timeout for Lambda functions in seconds. */
  protected final def DefaultTimeoutInSeconds: Long = 15.minutes.toSeconds

  /** The default timeout for external connections. */
  protected final def DefaultConnectionTimeout: FiniteDuration = 5.seconds

  /** The default retry policy for failed events. */
  protected final def DefaultRetryPolicy: RetryPolicy = RetryPolicy.Default

  /** The default enablement state for function mappings. */
  protected final def DefaultEnabled: Boolean = true

  /** The default batch size for function mappings. */
  protected final def DefaultBatchSize: Int = 10

  /** The default maximum batching window for function mappings in seconds. */
  protected final def DefaultMaximumBatchingWindowInSeconds: Long = 0L

  // Names

  /** The name of the S3 bucket that stores the Lambda functions. */
  protected final def lambdaS3Bucket: String = s"${Application.toLowerCase}-lambda-functions"

  /**
   * Generates the S3 key for the specified Lambda function domain.
   *
   * @param domain The name of the Lambda function domain.
   * @return The S3 key for the specified Lambda function domain.
   */
  protected final def lambdaS3Key(domain: String): Json =
    join("", s"$domain/${Application.toLowerCase}-$domain-", ref(Version), ".jar")

  /** Returns the simple name of the specified class.
   *
   * @tparam T The type of class to return the simple name of.
   * @return The simple name of the specified class.
   */
  protected final def name[T: ClassTag]: String = summon[ClassTag[T]].runtimeClass.getSimpleName

  /**
   * Returns the full name of the specified class.
   *
   * @tparam T The type of class to return the full name of.
   * @return The full name of the specified class.
   */
  protected final def fullName[T: ClassTag]: String = summon[ClassTag[T]].runtimeClass.getName

  /**
   * Generates the name of the SQS queue associated with the specified type.
   *
   * @tparam T The type to return the name of the associated SQS queue for.
   * @return The name of the SQS queue associated with the specified type.
   */
  protected final def sqsQueueName[T: ClassTag]: String =
    s"$Application${name[T]}SQSQueue"

  /**
   * Generates the name of the Lambda function associated with the specified type.
   *
   * @tparam T The type to return the name of the associated Lambda function for.
   * @return The name of the Lambda function associated with the specified type.
   */
  protected final def lambdaFunctionName[T: ClassTag]: String =
    s"$Application${name[T]}LambdaFunction"

  /**
   * Generates the name of the mapping associated with the specified type.
   *
   * @tparam T The type to return the name of the associated mapping for.
   * @return The name of the mapping associated with the specified type.
   */
  protected final def eventSourceMappingName[T: ClassTag]: String =
    s"$Application${name[T]}SourceMapping"

  /**
   * Returns the ARN for the specified service and name.
   *
   * @param service The service to resolve.
   * @param name    The name to resolve.
   * @return The ARN for the specified service and name.
   */
  protected final def arn(service: String, name: Json): Json =
    join(":",
      s"arn:aws",
      service,
      ref("AWS::Region"),
      ref("AWS::AccountId"),
      name
    )

  // Functions

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
  protected final def ref(target: String): Json =
    obj("Ref" -> target)

  /**
   * Generates JSON that resolves the specified SecretsManager key.
   *
   * @param key The SecretsManager key to resolve.
   * @return JSON that resolves the specified SecretsManager key.
   */
  protected final def resolveSecret(key: String): Json =
    fromString(s"{{resolve:secretsmanager:$Application:SecretString:$key}}")

  // Parameters

  /**
   * Generates a parameter definition.
   *
   * @tparam T The type of the parameter.
   * @param name        The name of the parameter.
   * @param description The description of the parameter.
   * @return A string parameter definition.
   */
  protected final def parameter[T: Parameter](name: String, description: String): Entry =
    parameter(name, description, None)

  /**
   * Generates a parameter definition.
   *
   * @tparam T The type of the parameter.
   * @param name        The name of the parameter.
   * @param description The description of the parameter.
   * @param default     The default value to use if none is provided.
   * @return A string parameter definition.
   */
  protected final def parameter[T: Parameter](name: String, description: String, default: T): Entry =
    parameter(name, description, Option(default))

  /**
   * Generates a parameter definition.
   *
   * @tparam T The type of the parameter.
   * @param name        The name of the parameter.
   * @param description The description of the parameter.
   * @param default     The optional default value to use if none is provided.
   * @return A string parameter definition.
   */
  protected final def parameter[T: Parameter](name: String, description: String, default: Option[T] = None): Entry =
    val parameter = Parameter[T]
    val basic = ListMap(Type -> Json.fromString(parameter._type), Description -> Json.fromString(description))
    name -> obj(default.map(parameter).fold(basic)(_default => basic + (Default -> Json.fromString(_default))).toSeq *)

  // Factories

  /**
   * Generates an SQS queue resource definition.
   *
   * @tparam T The type of event the queue manages.
   * @param visibilityTimeout The default visibility timeout for events in the queue.
   * @param tags              The tags to apply.
   * @return An SQS queue resource definition.
   */
  private final def sqsQueue[T: ClassTag](visibilityTimeout: Json, tags: Tag*): Entries =
    val queueName = sqsQueueName[T]
    Entries(
      queueName -> obj(
        Type -> "AWS::SQS::Queue",
        Properties -> obj(
          QueueName -> queueName,
          VisibilityTimeout -> visibilityTimeout,
          tagging[T](tags *)
        )
      )
    )

  /**
   * Generates a Lambda function resource definition.
   *
   * @tparam T The type of event the function handles.
   * @param description The description of the function.
   * @param s3Bucket    The description of the S3 bucket that contains the code.
   * @param s3Key       The description of the S3 key that contains the code.
   * @param handler     The name of the handler that implements the function.
   * @param memorySize  The amount of memory available to the function in MB, defaults to 128.
   * @param timeout     The amount of time the function can run in seconds, defaults to 30.
   * @param environment The environment that the function executes in.
   * @param tags        The tags to apply.
   * @return A Lambda function resource definition.
   */
  private final def lambdaFunction[T: ClassTag](
    description: String,
    s3Bucket: Json,
    s3Key: Json,
    handler: Json,
    memorySize: Json,
    timeout: Json,
    environment: Map[String, Json],
    tags: Tag*
  ): Entries =
    val functionName = lambdaFunctionName[T]
    Entries(
      functionName -> obj(
        Type -> "AWS::Lambda::Function",
        Properties -> obj(
          FunctionName -> functionName,
          Description -> description,
          Runtime -> "java11",
          Code -> obj("S3Bucket" -> s3Bucket, "S3Key" -> s3Key),
          Handler -> handler,
          MemorySize -> memorySize,
          Timeout -> timeout,
          Role -> join("",
            "arn:aws:iam::",
            ref("AWS::AccountId"),
            ":role/LoremasterLambdaIAMRole"
          ),
          Environment -> obj(Variables -> obj(environment.toSeq *)),
          tagging[T](tags *)
        )
      )
    )

  /**
   * Generates an SQS to Lambda event source mapping definition.
   *
   * @tparam T The type of event the mapping manages.
   * @param enabled                        The enablement state of the mapping.
   * @param batchSize                      The batch size of the mapping.
   * @param maximumBatchingWindowInSeconds The maximum batching window of the mapping.
   * @return An SQS to Lambda event source mapping definition.
   */
  private final def sqsToLambdaMapping[T: ClassTag](
    enabled: Json,
    batchSize: Json,
    maximumBatchingWindowInSeconds: Json
  ): Entries =
    val functionName = lambdaFunctionName[T]
    Entries(
      eventSourceMappingName[T] -> obj(
        Type -> "AWS::Lambda::EventSourceMapping",
        Properties -> obj(
          Enabled -> enabled,
          BatchSize -> batchSize,
          MaximumBatchingWindowInSeconds -> maximumBatchingWindowInSeconds,
          EventSourceArn -> arn("sqs", sqsQueueName[T]),
          FunctionName -> arn("lambda", functionName)
        ),
        "DependsOn" -> functionName
      )
    )

  /**
   * Returns the specified event and supplemental tags as a tag entry.
   *
   * @tparam T The type of event that is tagged.
   * @param tags The supplemental tags.
   * @return The specified event and supplemental tags as a tag entry.
   */
  private final def tagging[T: ClassTag](tags: Tag*): Entry =
    Tags -> arr((("Application" -> Application) +: ("Event" -> name[T]) +: tags).map {
      case (key, value) => obj(Key -> fromString(key), Value -> fromString(value))
    } *)

  /**
   * Generates a handler for SQS messages of type `T` with a Lambda function of type `F`.
   *
   * @tparam T The type of SQS messages to handle.
   * @tparam F The type of the function handler implementation.
   * @param domain                         The domain that contains the function.
   * @param description                    The description of the function.
   * @param memorySize                     The amount of memory available to the function in MB.
   * @param timeout                        The amount of time the function can run in seconds.
   * @param environment                    The environment that the function executes in.
   * @param enabled                        The enablement state of the mapping.
   * @param batchSize                      The batch size of the mapping.
   * @param maximumBatchingWindowInSeconds The maximum batching window of the mapping.
   * @param tags                           The tags to apply.
   * @return A handler for SQS messages of type `T` with a Lambda function of type `F`.
   */
  protected final def sqsQueueDeliversToLambdaFunction[T: ClassTag, F: ClassTag](
    description: String,
    domain: String,
    memorySize: Json,
    timeout: Json,
    environment: Map[String, Json],
    enabled: Json,
    batchSize: Json,
    maximumBatchingWindowInSeconds: Json,
    tags: Tag*
  ): Entries =
    sqsQueue[T](
      timeout,
      tags *
    ) ++ lambdaFunction[T](
      description,
      lambdaS3Bucket,
      lambdaS3Key(domain),
      fullName[F],
      memorySize,
      timeout,
      environment,
      tags *
    ) ++ sqsToLambdaMapping[T](
      enabled,
      batchSize,
      maximumBatchingWindowInSeconds
    )

  // Implementation

  /** Generates the parameters required by this deployment. */
  def parameters: Entries = Entries.empty +
    parameter[String](Version, Messages.version)

  /** Generates the resources provided by this deployment. */
  def resources: Entries = Entries.empty

/**
 * Definitions associated with deployments.
 */
object Deployment:

  /** The type of individual tags. */
  type Tag = (String, String)

  /** Factory for individual tags. */
  object Tag extends ((String, String) => Tag) :

    /* Create an individual tag. */
    override def apply(key: String, value: String): Tag = key -> value

  /** The type of individual deployment entries. */
  type Entry = (String, Json)

  /** Factory for individual deployment entries. */
  object Entry extends ((String, Json) => Entry) :

    /* Create an individual deployment entry. */
    override def apply(key: String, value: Json): Entry = key -> value

  /** The type of deployment entry collections. */
  type Entries = ListMap[String, Json]

  /** Factory for deployment entry collections. */
  object Entries:

    /** The empty deployment entry collection. */
    def empty: Entries = ListMap.empty

    /**
     * Generates an entry collection from the specified entries.
     *
     * @param entries The entries to generate a collection of.
     * @return An entry collection from the specified entries.
     */
    def apply(entries: Entry*): Entries = ListMap from entries
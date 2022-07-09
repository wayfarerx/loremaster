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
package aws

import java.util.{Map => JMap}

import scala.collection.immutable.ListMap
import scala.concurrent.duration.*
import scala.language.implicitConversions
import scala.reflect.ClassTag

import io.circe.Json

import event.*

/**
 * Base type for participants in the deployment process.
 */
trait Deployment:

  // Given Conversions

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

  /** The default enablement state for function mappings. */
  protected final def DefaultEnabled: Boolean = true

  /** The default batch size for function mappings. */
  protected final def DefaultBatchSize: Int = 10

  /** The default maximum batching window for function mappings in seconds. */
  protected final def DefaultMaximumBatchingWindowInSeconds: Long = 0L

  // Names

  /** The name of the S3 bucket that stores the Lambda functions. */
  protected final def lambdaS3Bucket: String =
    s"${Application.toLowerCase}-lambda-functions"

  /**
   * Generates the S3 key for the specified Lambda function domain.
   *
   * @param domain The name of the Lambda function domain.
   * @return The S3 key for the specified Lambda function domain.
   */
  protected final def lambdaS3Key(domain: String): Json =
    val domainLowerCase = domain.toLowerCase
    join(
      "",
      domainLowerCase,
      "/",
      Application.toLowerCase,
      "-",
      domainLowerCase,
      "-",
      ref(Version),
      ".jar"
    )

  /** Returns the simple name of the specified class.
   *
   * @tparam T The type of class to return the simple name of.
   * @return The simple name of the specified class.
   */
  protected final def name[T: ClassTag]: String =
    summon[ClassTag[T]].runtimeClass.getSimpleName

  /**
   * Returns the full name of the specified class.
   *
   * @tparam T The type of class to return the full name of.
   * @return The full name of the specified class.
   */
  protected final def fullName[T: ClassTag]: String =
    summon[ClassTag[T]].runtimeClass.getName

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
   * @param suffix The optional suffix to append to the name.
   * @return The name of the Lambda function associated with the specified type.
   */
  protected final def lambdaFunctionName[T: ClassTag](suffix: Option[String] = None): String =
    s"$Application${name[T]}${suffix getOrElse ""}LambdaFunction"

  /**
   * Generates the name of the mapping associated with the specified type.
   *
   * @tparam T The type to return the name of the associated mapping for.
   * @return The name of the mapping associated with the specified type.
   */
  protected final def eventSourceMappingName[T: ClassTag]: String =
    s"$Application${name[T]}SourceMapping"

  /**
   * Returns the batch size parameter name for the specified prefix.
   *
   * @param prefix The prefix to prepend to the configuration name.
   * @return The batch size parameter name for the specified prefix.
   */
  protected final def batchSizeName(prefix: String): String =
    s"$prefix$BatchSize"

  /**
   * Returns the enabled parameter name for the specified prefix.
   *
   * @param prefix The prefix to prepend to the configuration name.
   * @return The enabled parameter name for the specified prefix.
   */
  protected final def enabledName(prefix: String): String =
    s"$prefix$Enabled"

  /**
   * Returns the maximum batching window parameter name for the specified prefix.
   *
   * @param prefix The prefix to prepend to the configuration name.
   * @return The maximum batching window parameter name for the specified prefix.
   */
  protected final def maximumBatchingWindowInSecondsName(prefix: String): String =
    s"$prefix$MaximumBatchingWindowInSeconds"

  /**
   * Returns the memory size parameter name for the specified prefix.
   *
   * @param prefix The prefix to prepend to the configuration name.
   * @return The memory size parameter name for the specified prefix.
   */
  protected final def memorySizeInMBName(prefix: String): String =
    s"$prefix$MemorySizeInMB"

  /**
   * Returns the timeout parameter name for the specified prefix.
   *
   * @param prefix The prefix to prepend to the configuration name.
   * @return The timeout parameter name for the specified prefix.
   */
  protected final def timeoutInSecondsName(prefix: String): String =
    s"$prefix$TimeoutInSeconds"

  /**
   * Returns the trigger lambda function name for the specified domain.
   *
   * @param domain The name of the domain of the trigger lambda function.
   * @return The trigger lambda function name for the specified domain.
   */
  protected def triggerLambdaFunctionName(domain: String): String =
    s"$domain$Trigger"

  /**
   * Returns the handler lambda function name for the specified domain.
   *
   * @param domain The name of the domain of the handler lambda function.
   * @return The handler lambda function name for the specified domain.
   */
  protected def handlerLambdaFunctionName(domain: String): String =
    s"$domain$Handler"

  /**
   * Returns the ARN for the specified service and name.
   *
   * @param service The service to resolve.
   * @param name    The name to resolve.
   * @return The ARN for the specified service and name.
   */
  protected final def arn(service: Json, name: Json): Json =
    join(":",
      "arn:aws",
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
  protected final def join(delimiter: Json, values: Json*): Json =
    Json.obj("Fn::Join" -> Json.arr(delimiter, Json.arr(values *)))

  /**
   * Generates JSON that resolves the specified reference.
   *
   * @param target The name of the reference to resolve.
   * @return JSON that resolves the specified reference.
   */
  protected final def ref(target: Json): Json =
    Json.obj("Ref" -> target)

  /**
   * Generates JSON that resolves the specified SecretsManager key.
   *
   * @param key The SecretsManager key to resolve.
   * @return JSON that resolves the specified SecretsManager key.
   */
  protected final def resolveSecret(key: String): Json =
    Json.fromString(s"{{resolve:secretsmanager:$Application:SecretString:$key}}")

  // Parameters

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
    val basic = Entries(Type -> Json.fromString(parameter._type), Description -> Json.fromString(description))
    name -> Json.obj(default.map(parameter).fold(basic) {
      _default => basic + (Default -> Json.fromString(_default))
    }.toSeq *)

  /**
   * Generates the trigger Lambda parameter definitions for the specified domain.
   *
   * @param domain The domain to generate the trigger Lambda parameter definitions for.
   * @return The trigger Lambda parameter definitions for the specified domain.
   */
  protected final def triggerLambdaFunctionParameters(domain: String): Entries =
    val functionName = triggerLambdaFunctionName(domain)
    Entries(
      parameter(memorySizeInMBName(functionName), Messages.memorySizeInMB(functionName), DefaultMemorySizeInMB),
      parameter(timeoutInSecondsName(functionName), Messages.timeoutInSeconds(functionName), DefaultTimeoutInSeconds)
    )

  /**
   * Generates the SQS to Lambda parameter definitions for the specified domain.
   *
   * @param domain The domain to generate the SQS to Lambda parameter definitions for.
   * @return The SQS to Lambda parameter definitions for the specified domain.
   */
  protected final def handlerLambdaFunctionParameters(domain: String): Entries =
    val functionName = handlerLambdaFunctionName(domain)
    Entries(
      parameter(memorySizeInMBName(functionName), Messages.memorySizeInMB(functionName), DefaultMemorySizeInMB),
      parameter(timeoutInSecondsName(functionName), Messages.timeoutInSeconds(functionName), DefaultTimeoutInSeconds),
      parameter(enabledName(functionName), Messages.enabled(functionName), DefaultEnabled),
      parameter(batchSizeName(functionName), Messages.batchSize(functionName), DefaultBatchSize),
      parameter(
        maximumBatchingWindowInSecondsName(functionName),
        Messages.maximumBatchingWindowInSeconds(functionName),
        DefaultMaximumBatchingWindowInSeconds
      )
    )

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
      queueName -> Json.obj(
        Type -> "AWS::SQS::Queue",
        Properties -> Json.obj(
          QueueName -> queueName,
          VisibilityTimeout -> visibilityTimeout,
          tagged(tags *)
        )
      )
    )

  /**
   * Generates a Lambda function resource definition.
   *
   * @param functionName The name of the Lambda function.
   * @param description  The description of the function.
   * @param s3Bucket     The description of the S3 bucket that contains the code.
   * @param s3Key        The description of the S3 key that contains the code.
   * @param handler      The name of the handler that implements the function.
   * @param memorySize   The amount of memory available to the function in MB, defaults to 128.
   * @param timeout      The amount of time the function can run in seconds, defaults to 30.
   * @param environment  The environment that the function executes in.
   * @param tags         The tags to apply.
   * @return A Lambda function resource definition.
   */
  private final def lambdaFunction(
    functionName: String,
    description: String,
    s3Bucket: Json,
    s3Key: Json,
    handler: Json,
    memorySize: Json,
    timeout: Json,
    environment: Map[String, Json],
    tags: Tag*
  ): Entries = Entries(
    functionName -> Json.obj(
      Type -> "AWS::Lambda::Function",
      Properties -> Json.obj(
        FunctionName -> functionName,
        Description -> description,
        Runtime -> "java11",
        Code -> Json.obj("S3Bucket" -> s3Bucket, "S3Key" -> s3Key),
        Handler -> handler,
        MemorySize -> memorySize,
        Timeout -> timeout,
        Role -> join("",
          "arn:aws:iam::",
          ref("AWS::AccountId"),
          s":role/${Application}LambdaIAMRole"
        ),
        Environment -> Json.obj(Variables -> Json.obj(environment.toSeq *)),
        tagged(tags *)
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
    maximumBatchingWindowInSeconds: Json,
  ): Entries =
    val functionName = lambdaFunctionName[T]()
    Entries(
      eventSourceMappingName[T] -> Json.obj(
        Type -> "AWS::Lambda::EventSourceMapping",
        Properties -> Json.obj(
          Enabled -> enabled,
          BatchSize -> batchSize,
          MaximumBatchingWindowInSeconds -> maximumBatchingWindowInSeconds,
          EventSourceArn -> arn("sqs", sqsQueueName[T]),
          FunctionName -> arn("lambda", functionName)
        ),
        "DependsOn" -> functionName
      )
    )

  private final def scheduleRule(domain: String, description: String) = Entries(
    triggerLambdaFunctionName(domain) -> Json.obj(
      Type -> "AWS::Events::Rule",
      Properties -> Json.obj(
        Description -> description,
        "ScheduleExpression" -> ref("ScheduleExpression"),
        "State" -> ref("State"),
        "Targets" -> ref("Targets")
      )
    )
  )

  /**
   * Returns the specified event and supplemental tags as a tag entry.
   *
   * @param tags The supplemental tags.
   * @return The specified event and supplemental tags as a tag entry.
   */
  private final def tagged(tags: Tag*): Entry =
    Tags -> Json.arr((("Application" -> Application) +: tags).map {
      case (key, value) => Json.obj(Key -> Json.fromString(key), Value -> Json.fromString(value))
    } *)

  /**
   * Generates a handler for triggered invocations with a Lambda function of type `F`.
   *
   * @tparam F The type of the function handler implementation.
   * @param domain      The domain of the function.
   * @param description The description of the function.
   * @param environment The environment that the function executes in.
   * @param tags        The tags to apply.
   * @return A handler for triggered invocations with a Lambda function of type `F`..
   */
  protected final def triggerLambdaFunction[F: ClassTag](
    domain: String,
    description: String,
    environment: Map[String, Json],
    tags: (String, String)*
  ): Entries =
    val functionName = triggerLambdaFunctionName(domain)
    lambdaFunction(
      functionName,
      description,
      lambdaS3Bucket,
      lambdaS3Key(domain),
      fullName[F],
      ref(memorySizeInMBName(functionName)),
      ref(timeoutInSecondsName(functionName)),
      environment,
      (Domain, domain) +: tags *
    ) ++ scheduleRule(functionName, "")

  /**
   * Generates a handler for SQS messages of type `E` with a Lambda function of type `F`.
   *
   * @tparam E The type of SQS events to handle.
   * @tparam F The type of the function handler implementation.
   * @param domain      The domain of the function.
   * @param description The description of the function.
   * @param environment The environment that the function executes in.
   * @param tags        The tags to apply.
   * @return A handler for SQS messages of type `T` with a Lambda function of type `F`.
   */
  protected final def handlerLambdaFunction[E: ClassTag, F: ClassTag](
    domain: String,
    description: String,
    environment: Map[String, Json],
    tags: (String, String)*
  ): Entries =
    val functionName = handlerLambdaFunctionName(domain)
    val timeout = ref(timeoutInSecondsName(functionName))
    val _tags = (Domain, domain) +: tags
    sqsQueue[E](
      timeout,
      _tags *
    ) ++ lambdaFunction(
      functionName,
      description,
      lambdaS3Bucket,
      lambdaS3Key(domain),
      fullName[F],
      ref(memorySizeInMBName(functionName)),
      timeout,
      environment,
      _tags *
    ) ++ sqsToLambdaMapping[E](
      ref(enabledName(functionName)),
      ref(batchSizeName(functionName)),
      ref(maximumBatchingWindowInSecondsName(functionName))
    )

  // Implementation

  /** Generates the parameters required by this deployment. */
  def parameters: Entries = Entries(parameter[String](Version, Messages.version))

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
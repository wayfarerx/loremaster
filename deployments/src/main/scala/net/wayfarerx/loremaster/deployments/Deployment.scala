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
import scala.language.implicitConversions
import scala.reflect.ClassTag

import io.circe.{Encoder, Json}
import Json.{arr, fromString, obj}

/**
 * Base type for participants in the deployment process.
 */
trait Deployment:

  /** The given conversion of integers to JSON integers. */
  protected final given Conversion[Int, Json] = Deployment.asJson

  /** The given conversion of strings to JSON strings. */
  protected final given Conversion[String, Json] = Deployment.asJson

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

  /** The default timeout for external connections in seconds. */
  protected def defaultConnectionTimeout: Int = Deployment.defaultConnectionTimeout

  /** The default memory size for Lambda functions in megabytes. */
  protected def defaultFunctionMemorySize: Int = Deployment.defaultFunctionMemorySize

  /** The default timeout for Lambda functions in seconds. */
  protected def defaultFunctionTimeout: Int = Deployment.defaultFunctionTimeout

  /** Returns the simple name of the specified class.
   *
   * @tparam T The type of class to return the simple name of.
   * @return The simple name of the specified class.
   */
  protected final def name[T: ClassTag]: String = Deployment.name[T]

  /**
   * Returns the full name of the specified class.
   *
   * @tparam T The type of class to return the full name of.
   * @return The full name of the specified class.
   */
  protected final def fullName[T: ClassTag]: String = Deployment.fullName[T]

  /**
   * Generates the tags for a resource.
   *
   * @tparam T The type of event to tag.
   * @param tags The tags to apply.
   * @return Tags for a resource.
   */
  protected final def generateTags[T: ClassTag](tags: Tag*): Json = Deployment.generateTags[T](tags *)

  /** A type class for supported parameter types. */
  protected final type Parameter[T] = Deployment.Parameter[T]

  /** Factory for supported parameter types. */
  protected final def Parameter: Deployment.Parameter.type = Deployment.Parameter

  /** Permission to perform one or more actions. */
  protected final type Permission = Deployment.Permission

  /** Factory for permissions. */
  protected final def Permission: Deployment.Permission.type = Deployment.Permission

  // Names

  /**
   * Generates the name of the IAM role with the specified prefix.
   *
   * @tparam T The type to return the name of the associated IAM role for.
   * @return The name of the IAM role with the specified prefix.
   */
  protected final def iamRoleName[T: ClassTag]: String = s"$Application${name[T]}IAMRole"

  /**
   * Generates the name of the Lambda function associated with the specified type.
   *
   * @tparam T The type to return the name of the associated Lambda function for.
   * @return The name of the Lambda function associated with the specified type.
   */
  protected final def lambdaFunctionName[T: ClassTag]: String = s"$Application${name[T]}LambdaFunction"

  /**
   * Generates the name of the SQS queue associated with the specified type.
   *
   * @tparam T The type to return the name of the associated SQS queue for.
   * @return The name of the SQS queue associated with the specified type.
   */
  protected final def sqsQueueName[T: ClassTag]: String = s"$Application${name[T]}SQSQueue"

  /**
   * Generates the name of the mapping associated with the specified type.
   *
   * @tparam T The type to return the name of the associated mapping for.
   * @return The name of the mapping associated with the specified type.
   */
  protected final def eventSourceMappingName[T: ClassTag]: String = s"$Application${name[T]}EventSourceMapping"

  // Functions

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
   * Generates an AWS IAM role resource definition.
   *
   * @tparam T The type of event the role manages.
   * @param permissions       The permission that are granted.
   * @param assumePermissions The permissions that apply to assuming the role.
   * @param tags              The tags to apply.
   * @return An AWS Lambda IAM role resource definition.
   */
  protected final def iamRole[T: ClassTag](
    permissions: Seq[Permission],
    assumePermissions: Seq[Permission],
    tags: Tag*
  ): Entries =
    val roleName = iamRoleName[T]
    Entries(
      roleName -> obj(
        Type -> "AWS::IAM::Role",
        Properties -> obj(
          RoleName -> roleName,
          Policies -> arr(obj(
            PolicyName -> s"$roleName$Policy",
            PolicyDocument -> obj(
              Version -> _2012_10_17,
              Statement -> arr(permissions.map(Encoder[Permission].apply) *)
            )
          )),
          AssumeRolePolicyDocument -> obj(
            Version -> _2012_10_17,
            Statement -> arr(assumePermissions.map(Encoder[Permission].apply) *)
          ),
          Tags -> generateTags[T](tags *)
        )
      )
    )

  /**
   * Generates an IAM role and Lambda function resource definition.
   *
   * @tparam T The type of event the function handles.
   * @param description The description of the function.
   * @param handler     The name of the handler that implements the function.
   * @param s3Bucket    The description of the S3 bucket that contains the code.
   * @param s3Key       The description of the S3 key that contains the code.
   * @param memorySize  The amount of memory available to the function in MB, defaults to 128.
   * @param timeout     The amount of time the function can run in seconds, defaults to 30.
   * @param permissions The permissions that the function is granted.
   * @param environment The environment that the function executes in.
   * @param tags        The tags to apply.
   * @return An IAM role and Lambda function resource definition.
   */
  protected final def lambdaFunction[T: ClassTag](
    description: String,
    s3Bucket: Json,
    s3Key: Json,
    handler: String,
    memorySize: Json,
    timeout: Json,
    permissions: Seq[Permission],
    environment: Map[String, Json],
    tags: Tag*
  ): Entries =
    val roleName = iamRoleName[T]
    val functionName = lambdaFunctionName[T]
    iamRole[T](
      Seq(Permission(Seq("logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"), "*")) ++ permissions,
      Seq(Permission("sts:AssumeRole", Map(Service -> Seq("lambda.amazonaws.com")))),
      tags *
    ) ++ Entries(
      functionName -> obj(
        Type -> "AWS::Lambda::Function",
        Properties -> obj(
          FunctionName -> functionName,
          Description -> description,
          Runtime -> "java11",
          Handler -> handler,
          MemorySize -> memorySize,
          Timeout -> timeout,
          Code -> obj("S3Bucket" -> s3Bucket, "S3Key" -> s3Key),
          Role -> join("", "arn:aws:iam::", ref(AccountID), s":role/${iamRoleName[T]}"),
          Environment -> obj(Variables -> obj(environment.toSeq *)),
          Tags -> generateTags[T](tags *)
        ),
        DependsOn -> arr(roleName)
      )
    )

  /**
   * Generates an AWS SQS queue resource definition.
   *
   * @tparam T The type of event the queue manages.
   * @param visibilityTimeout The number of seconds a message is visible after it is delivered from the queue.
   * @param tags              The tags to apply.
   * @return An AWS SQS queue resource definition.
   */
  protected final def sqsQueue[T: ClassTag](visibilityTimeout: Json, tags: Tag*): Entries =
    val queueName = sqsQueueName[T]
    Entries(
      queueName -> obj(
        Type -> "AWS::SQS::Queue",
        Properties -> obj(
          QueueName -> queueName,
          VisibilityTimeout -> visibilityTimeout,
          Tags -> generateTags[T](tags *)
        )
      )
    )

  /**
   * Generates an SQS to Lambda event source mapping definition.
   *
   * @tparam T The type of event the mapping manages.
   * @return An SQS to Lambda event source mapping definition.
   */
  protected final def sqsToLambdaMapping[T: ClassTag]: Entries = Entries(
    eventSourceMappingName[T] -> obj(
      Type -> "AWS::Lambda::EventSourceMapping",
      Properties -> obj(
        EventSourceArn -> getAtt(sqsQueueName[T], Arn),
        FunctionName -> ref(lambdaFunctionName[T])
      )
    )
  )

  /**
   * Generates an SQS queue, IAM role, Lambda function and event source mapping definition.
   *
   * @tparam T The type of event the queue and function handle.
   * @tparam F The type of the function handler implementation.
   * @param description The description of the function.
   * @param s3Bucket    The description of the S3 bucket that contains the code.
   * @param s3Key       The description of the S3 key that contains the code.
   * @param memorySize  The amount of memory available to the function in MB, defaults to 128.
   * @param timeout     The amount of time the function can run in seconds, defaults to 30.
   * @param permissions The permissions that the function is granted.
   * @param environment The environment that the function executes in.
   * @param tags        The tags to apply.
   * @return An SQS queue, IAM role, Lambda function and event source mapping definition.
   */
  protected final def sqsQueueToLambdaFunction[T: ClassTag, F: ClassTag](
    description: String,
    s3Bucket: Json,
    s3Key: Json,
    memorySize: Json,
    timeout: Json,
    permissions: Seq[Permission],
    environment: Map[String, Json],
    tags: Tag*
  ): Entries =
    sqsQueue[T](timeout, tags *) ++ lambdaFunction[T](
      description,
      s3Bucket,
      s3Key,
      fullName[F],
      memorySize,
      timeout,
      Permission("sqs:*", getAtt(sqsQueueName[T], Arn)) +: permissions,
      environment,
      tags *
    ) ++ sqsToLambdaMapping[T]

  // Implementation

  /** Generates the parameters required by this deployment. */
  def parameters: Entries = Entries.empty + parameter[String](AccountID, Messages.accountId)

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

  /**
   * Converts an integer to JSON.
   *
   * @param value The value to convert.
   * @return An integer converted to JSON.
   */
  def asJson(value: Int): Json = Json.fromInt(value)

  /**
   * Converts a string to JSON.
   *
   * @param value The value to convert.
   * @return A string converted to JSON.
   */
  def asJson(value: String): Json = Json.fromString(value)

  /**
   * Converts non-empty sequences of strings to JSON.
   *
   * @param value The value to convert.
   * @return A non-empty sequence of strings converted to JSON.
   */
  def asJson(value: Seq[String]): Option[Json] =
    if value.isEmpty then None
    else if value.size == 1 then Some(asJson(value.head))
    else Some(arr(value.map(asJson) *))

  // Defaults

  /** The default timeout for external connections in seconds. */
  def defaultConnectionTimeout: Int = 5

  /** The default memory size for Lambda functions in megabytes. */
  def defaultFunctionMemorySize: Int = 1024

  /** The default timeout for Lambda functions in seconds. */
  def defaultFunctionTimeout: Int = 900

  // Names

  /**
   * Returns the simple name of the specified class.
   *
   * @tparam T The type of class to return the simple name of.
   * @return The simple name of the specified class.
   */
  private def name[T: ClassTag]: String =
    summon[ClassTag[T]].runtimeClass.getSimpleName

  /**
   * Returns the full name of the specified class.
   *
   * @tparam T The type of class to return the full name of.
   * @return The full name of the specified class.
   */
  private def fullName[T: ClassTag]: String =
    summon[ClassTag[T]].runtimeClass.getName

  /**
   * Generates the tags for a resource.
   *
   * @tparam T The type of event to tag.
   * @param tags The tags to apply.
   * @return Tags for a resource.
   */
  private def generateTags[T: ClassTag](tags: Tag*): Json =
    arr((("Application" -> Application) +: (Event -> name[T]) +: tags).map {
      case (key, value) => obj(Key -> fromString(key), Value -> fromString(value))
    } *)

  /**
   * A type class for supported parameter types.
   *
   * @tparam T The type of parameter that is supported.
   */
  sealed trait Parameter[T] extends (T => String) :

    /** The AWS type of the parameter. */
    def _type: String

  /**
   * Factory for supported parameter types.
   */
  object Parameter:

    /** The given integer parameter type support. */
    given Parameter[Int] = Implementation("Number", _.toString)

    /** The given string parameter type support. */
    given Parameter[String] = Implementation("String", identity)

    /**
     * Summons the support for the specified parameter type.
     *
     * @tparam T The type of parameter to summon support for.
     * @return The support for the specified parameter type.
     */
    def apply[T: Parameter]: Parameter[T] = summon[Parameter[T]]

    /**
     * The implementation of the parameter type class.
     *
     * @tparam T The type of parameter to support.
     * @param _type  The name of the supported type.
     * @param encode The function that encodes supported values.
     */
    private final class Implementation[T](override val _type: String, encode: T => String) extends Parameter[T] :

      /* Encode the specified value. */
      override def apply(value: T): String = encode(value)

  /**
   * Permission to perform actions.
   *
   * @param actions   The actions to allow.
   * @param resource  The optional resource to restrict this permission to.
   * @param principal The optional principal to restrict this permission to.
   */
  case class Permission(
    actions: Seq[String],
    resource: Option[Json] = None,
    principal: Option[Map[String, Seq[String]]] = None
  )

  /**
   * Factory for permissions.
   */
  object Permission extends ((Seq[String], Option[Json], Option[Map[String, Seq[String]]]) => Permission) :

    /** The encoding of permissions to JSON. */
    given Encoder[Permission] = { permission =>
      val withEffect = obj(Effect -> asJson(Allow))
      val withAction = asJson(permission.actions).map(a => obj(Action -> a)).fold(withEffect)(withEffect.deepMerge)
      val withResource = permission.resource.map(r => obj(Resource -> r)).fold(withAction)(withAction.deepMerge)
      val withPrincipal = permission.principal.map { principal =>
        obj(Principal -> obj(principal.flatMap { case (key, values) => asJson(values) map (key -> _) }.toSeq *))
      }.fold(withResource)(withResource.deepMerge)
      withPrincipal
    }

    /**
     * Creates permission to perform an action.
     *
     * @param action The action to allow.
     */
    def apply(action: String): Permission =
      apply(Seq(action), None, None)

    /**
     * Creates permission to perform an action.
     *
     * @param action   The action to allow.
     * @param resource The resource to restrict the permission to.
     */
    def apply(action: String, resource: Json): Permission =
      apply(Seq(action), Some(resource), None)

    /**
     * Creates permission to perform an action.
     *
     * @param action    The action to allow.
     * @param principal The principal to restrict the permission to.
     */
    def apply(action: String, principal: Map[String, Seq[String]]): Permission =
      apply(Seq(action), None, Some(principal))

    /**
     * Creates permission to perform an action.
     *
     * @param action    The action to allow.
     * @param resource  The resource to restrict the permission to.
     * @param principal The principal to restrict the permission to.
     */
    def apply(action: String, resource: Json, principal: Map[String, Seq[String]]): Permission =
      apply(Seq(action), Some(resource), Some(principal))

    /**
     * Creates permission to perform the specified actions.
     *
     * @param actions  The actions to allow.
     * @param resource The resource to restrict the permission to.
     */
    def apply(actions: Seq[String], resource: Json): Permission =
      apply(actions, Some(resource), None)

    /**
     * Creates permission to perform the specified actions.
     *
     * @param actions   The actions to allow.
     * @param principal The principal to restrict the permission to.
     */
    def apply(actions: Seq[String], principal: Map[String, Seq[String]]): Permission =
      apply(actions, None, Some(principal))

    /**
     * Creates permission to perform the specified actions.
     *
     * @param actions   The actions to allow.
     * @param resource  The resource to restrict the permission to.
     * @param principal The principal to restrict the permission to.
     */
    def apply(actions: Seq[String], resource: Json, principal: Map[String, Seq[String]]): Permission =
      apply(actions, Some(resource), Some(principal))
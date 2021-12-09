/* KinesisFunction.scala
 *
 * Copyright (c) 2021 wayfarerx (@thewayfarerx).
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

import java.nio.charset.StandardCharsets

import scala.jdk.CollectionConverters.given

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.KinesisEvent

import io.circe.Decoder
import io.circe.parser.decode

import zio.{Has, RIO, RLayer, Runtime, Task, UIO, ZEnv, ZLayer}
import zio.system.System

import configuration.*
import logging.*

/**
 * Base type for AWS Kinesis Lambda functions that operate in the specified environment.
 *
 * @tparam E The environment this Lambda function operates in.
 * @tparam T The type of event this Lambda function handles.
 * @param environment The environment constructor to use.
 */
abstract class KinesisFunction[E <: ZEnv, T: Decoder](
  environment: RLayer[ZEnv & Has[Configuration] & Has[LogFactory], E] = ZLayer.identity
) extends RequestHandler[KinesisEvent, Unit] :

  /* Handle Lambda Kinesis events. */
  final override def handleRequest(event: KinesisEvent, context: Context): Unit =
    Runtime.default unsafeRunTask handle(event.getRecords.iterator.asScala.toList).provideLayer {
      ZLayer.requires[ZEnv] ++ ZLayer.succeed(context.getLogger) >>> KinesisFunction.HostLayer >>> environment
    }

  /**
   * Handles events from the Kinesis stream.
   *
   * @param events The events to handle.
   * @return A task that handles the specified events.
   */
  private def handle(events: List[KinesisEvent.KinesisEventRecord]): RIO[E, Unit] = events match
    case head :: tail =>
      for
        data <- Task(StandardCharsets.UTF_8.decode(head.getKinesis.getData).toString)
        event <- Task.fromEither(decode[T](data))
        _ <- apply(event)
        _ <- handle(tail)
      yield ()
    case Nil =>
      UIO.unit

  /**
   * Handles an event from the Kinesis stream.
   *
   * @param event The event to handle.
   * @return A task that handles the specified event.
   */
  def apply(event: T): RIO[E, Unit]

/**
 * Definitions associated with AWS Kinesis Lambda functions.
 */
object KinesisFunction:

  /** The layer provided to AWS Kinesis Lambda functions. */
  private val HostLayer: RLayer[ZEnv & Has[LambdaLogger], ZEnv & Has[Configuration] & Has[LogFactory]] =
    val config = ZLayer.fromService[System.Service, Configuration](Configuration apply _.env)
    val logs = config ++ ZLayer.requires[Has[LambdaLogger]] >>> AwsLogFactory.live
    ZLayer.requires[ZEnv] ++ config ++ logs
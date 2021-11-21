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

import scala.jdk.CollectionConverters.*

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.KinesisEvent

import io.circe.Decoder

import zio.Task

import configuration.*
import logging.*

import zio.{Has, RIO, Runtime, UIO, ZEnv, ZLayer}

trait KinesisFunction[T: Decoder] extends RequestHandler[KinesisEvent, Unit] :

  import KinesisFunction.*

  final override def handleRequest(event: KinesisEvent, context: Context): Unit =
    Runtime.default unsafeRunTask handle(event.getRecords.iterator.asScala.toList).provideCustomLayer(
      AwsConfiguration.live ++ (AwsConfiguration.live ++ ZLayer.succeed(context.getLogger) >>> AwsLogFactory.live)
    )

  /**
   * Handles events from the Kinesis stream.
   *
   * @param events The events to handle.
   * @return A task that handles the specified events.
   */
  private def handle(events: List[KinesisEvent.KinesisEventRecord]): RIO[LoremasterEnv, Unit] = events match {
    case head :: tail => ???
    case Nil => UIO.unit
  }

  /**
   * Handles an event from the Kinesis stream.
   *
   * @param event The event to handle.
   * @return A task that handles the specified event.
   */
  def apply(event: T): RIO[LoremasterEnv, Unit]

object KinesisFunction:

  type LoremasterEnv = ZEnv & Has[Configuration] & Has[LogFactory]


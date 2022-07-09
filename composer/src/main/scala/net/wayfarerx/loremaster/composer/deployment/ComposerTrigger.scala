/* ComposerTrigger.scala
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
package composer
package deployment

import cats.data.NonEmptyList

import zio.{Has, RIO, RLayer, UIO, ZLayer}

import aws.*
import event.*
import logging.*

/**
 * An AWS Lambda function that generates composer events.
 */
final class ComposerTrigger extends TriggeredFunction :

  /* The type of environment to use. */
  override type Environment = AwsEnv & Has[Publisher[ComposerEvent]]

  /* The environment constructor to use. */
  override def environment: RLayer[AwsEnv, Environment] =
    ZLayer.requires[AwsEnv] ++ ZLayer.fromEffect(UIO(SqsPublisher[ComposerEvent]))

  /* Schedule the specified composition. */
  override protected def onTrigger(invocation: Map[String, String]): RIO[EnvironmentWithLog, Unit] = for
    publisher <- RIO.service[Publisher[ComposerEvent]]
    composition <- compose(invocation)
    _ <- publisher(ComposerEvent(composition))
  yield ()

  /**
   * Generates a composition.
   *
   * @param event The event to generate from.
   * @return A new composition.
   */
  private def compose(event: Map[String, String]): RIO[EnvironmentWithLog, Composition] =
    UIO(Composition(NonEmptyList.one(1))) // FIXME Generate random compositions.
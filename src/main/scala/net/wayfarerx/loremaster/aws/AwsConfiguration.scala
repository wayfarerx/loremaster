/* AwsConfiguration.scala
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

import zio.{Has, RIO, RLayer, Task, UIO, ZLayer}
import zio.system.System

import configuration.*

/**
 * Implementation of the AWS configuration service.
 *
 * @param system The system service to use.
 */
final class AwsConfiguration(system: System.Service) extends Configuration :

  import Configuration.Data

  /* Return the value of the specified configuration entry if it exists. */
  override def get[T: Data](key: String): Task[Option[T]] =
    system env key map (_ flatMap Data[T].decode)

/**
 * Factory for AWS configurations.
 */
object AwsConfiguration extends (System.Service => AwsConfiguration):

  /** The live AWS configuration layer. */
  val live: RLayer[System, Has[Configuration]] = ZLayer.fromEffect(RIO.service[System.Service] map apply)

  /* Create a new AWS configuration. */
  override def apply(system: System.Service): AwsConfiguration = new AwsConfiguration(system)
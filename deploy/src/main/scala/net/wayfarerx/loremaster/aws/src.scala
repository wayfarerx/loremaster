/* src.scala
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

import zio.{Has, IO, RLayer, ZEnv, ZLayer}
import zio.system.System

import configuration.*
import logging.*

/** The type of environment that AWS effects operate in. */
type AwsEnv = ZEnv & Has[LogFactory] & Has[Configuration]

/** Factory for AWS environments. */
val AwsEnv: RLayer[ZEnv & Has[LogEmitter], AwsEnv] =
  val config = ZLayer.fromService { (sys: System.Service) =>
    Configuration { key =>
      sys.env(key) catchAll { thrown =>
        IO.fail(ConfigurationProblem(aws.Messages.securityException(thrown), Some(thrown)))
      }
    }
  }
  val logs = config ++ ZLayer.requires[Has[LogEmitter]] >>> ZLayer.fromServices(LogFactory(_, _))
  ZLayer.requires[ZEnv] ++ logs ++ config
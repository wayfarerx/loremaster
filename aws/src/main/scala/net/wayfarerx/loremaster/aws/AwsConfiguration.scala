/* AwsConfiguration.scala
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

import zio.{IO, UIO}
import zio.system.System

import configuration.*

/**
 * An implementation of the configuration service that uses environment variables.
 *
 * @param sys The system service to use when accessing environment variables.
 */
final class AwsConfiguration(sys: System.Service) extends Configuration :

  /* Return the value of the specified configuration entry if it exists in the environment. */
  override def get[T: Configuration.Data](key: String): ConfigurationEffect[Option[T]] = for
    value <- sys.env(key) catchAll { thrown =>
      IO.fail(ConfigurationProblem(Messages.unableToAccessConfigurationData(key), Some(thrown)))
    }
    result <- value.fold(UIO.none) {
      Configuration.Data[T].apply(_).fold {
        IO.fail(ConfigurationProblem(Messages.invalidConfigurationData(key, Configuration.Data[T].`type`)))
      }(UIO.some)
    }
  yield result

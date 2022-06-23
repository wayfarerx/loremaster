/* LambdaFunction.scala
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

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import zio.{Has, RLayer, Runtime, RIO, ZLayer}

import logging.*

/**
 * Base type for Lambda functions.
 *
 * @tparam T The type of request this Lambda function produces.
 */
trait LambdaFunction[T]:
  self: RequestHandler[T, String] =>

  /**
   * The type of environment to use.
   */
  type Environment <: AwsEnv

  /**
   * The type of environment being used with a log attached.
   */
  final type EnvironmentWithLog = Environment & Has[Log]

  /**
   * The environment constructor to use.
   */
  def environment: RLayer[AwsEnv, Environment]

  /**
   * The environment constructor to use with a log attached.
   */
  final def environmentWithLog: RLayer[AwsEnv, EnvironmentWithLog] =
    environment ++ ZLayer.fromEffect(RIO.service[Logging] flatMap (_ (getClass)))

  /**
   * Handles a Lambda request with the provided environment.
   *
   * @param request The request to handle.
   * @return The environment-dependant request handler.
   */
  def apply(request: T): RIO[EnvironmentWithLog, Unit]

  /* Handle a Lambda request. */
  final override def handleRequest(request: T, context: Context): String =
    Runtime.default unsafeRunTask apply(request).map(_ => Messages.okay).provideLayer {
      AwsEnv(context) >>> environmentWithLog
    }
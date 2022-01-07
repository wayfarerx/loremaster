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

import zio.{RIO, RLayer, Runtime, UIO, ZEnv, ZLayer}

import logging.*

/**
 * Base type for AWS Lambda functions that operate in the specified environment.
 *
 * @tparam E The environment this AWS Lambda function operates in.
 * @tparam R The type of request this AWS Lambda function handles.
 */
trait LambdaFunction[E <: AwsEnv, R] extends RequestHandler[R, String] with (R => RIO[E, Unit]):

  /** The environment constructor to use. */
  protected def environment: RLayer[AwsEnv, E]

  /* Handle a Lambda request. */
  final override def handleRequest(request: R, context: Context): String =
    val logger = context.getLogger
    Runtime.default unsafeRunTask {
      apply(request) map (_ => Messages.okay) provideLayer
        ZLayer.requires[ZEnv] ++ ZLayer.succeed(LogEmitter formatted (UIO apply logger.log(_))) >>>
          AwsEnv >>> environment
    }
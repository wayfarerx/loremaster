/* Main.scala
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
package main

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets

import io.circe.Json.{fromFields, fromString, obj}

import zio.{App, ExitCode, Task, UIO, URIO, ZEnv, ZManaged}
import zio.console

/**
 * A program that generates the AWS CloudFormation template.
 */
object Main extends App :

  /** The deployment to generate from. */
  private lazy val deployment =
    new twitter.deployment.TwitterDeployment {}

  /* Write the generated template to the specified file. */
  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    args match
      case file :: Nil =>
        ZManaged.fromAutoCloseable(Task(PrintWriter(File(file), StandardCharsets.UTF_8.toString))).use { writer =>
          Task {
            writer.write(emitJson(obj(
              "AWSTemplateFormatVersion" -> fromString("2010-09-09"),
              "Description" -> fromString(Messages.description),
              "Parameters" -> fromFields(deployment.parameters),
              "Resources" -> fromFields(deployment.resources)
            )))
          }
        }.map(_ => ExitCode.success) catchAll { thrown =>
          console.putStrLnErr(Messages.failedToWriteAwsCloudFormationTemplate(thrown)) *> UIO(ExitCode.failure)
        }
      case _ =>
        console.putStrLnErr(Messages.usage) *> UIO(ExitCode.failure)
  }.catchAll(UIO.die(_))
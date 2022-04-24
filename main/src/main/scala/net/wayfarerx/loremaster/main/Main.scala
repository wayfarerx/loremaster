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

import zio.{ExitCode, Task, Runtime, UIO, URIO, ZEnv, ZManaged}
import zio.console

/**
 * A program that generates the AWS CloudFormation template.
 */
object Main :

  def main(args: Array[String]): Unit =
    Runtime.default.unsafeRunTask {
      {
        args.toList match
          case Nil => ZManaged.succeed(System.out)
          case file :: Nil => ZManaged.fromAutoCloseable(Task(PrintWriter(File(file), StandardCharsets.UTF_8.toString)))
          case _ => ZManaged.fail(IllegalArgumentException(Messages.usage))
      }.use { output =>
        Task {
          val deployment =
            new twitter.deployment.TwitterDeployment {}
          output.append(emitJson(obj(
            "AWSTemplateFormatVersion" -> fromString("2010-09-09"),
            "Description" -> fromString(Messages.description),
            "Parameters" -> fromFields(deployment.parameters),
            "Resources" -> fromFields(deployment.resources)
          )))
        } catchAll { thrown =>
          console.putStrLnErr(Messages.failedToWriteAwsCloudFormationTemplate(thrown))
        }
      }
    }
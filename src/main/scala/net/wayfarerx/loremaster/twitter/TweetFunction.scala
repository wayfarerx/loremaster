/* TweetPublisher.scala
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
package twitter

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.KinesisEvent

import zio.Task

import aws.*
import logging.*

final class TweetFunction extends RequestHandler[KinesisEvent, Unit]:

  override def handleRequest(event: KinesisEvent, context: Context): Unit =
    val logFactory = AwsLogFactory(Log.Level.Warn, context.getLogger) // FIXME configure log threshold
    val publisher = TweetPublisher(logFactory, ???)

    def continue(records: List[KinesisEvent.KinesisEventRecord]): Task[Unit] = records match
      case head :: tail => ???
      case Nil => Task.unit

    event.getRecords
    ???

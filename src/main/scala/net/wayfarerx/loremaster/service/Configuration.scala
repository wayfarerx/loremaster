/* Configuration.scala
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
package service

import scala.concurrent.duration.FiniteDuration

/**
 * Definition of the configuration service.
 */
trait Configuration:

  /** The path or URI to use for storing persistant data. */
  def storage: String

  /** The designator that identifies the zeitgeist to use. */
  def zeitgeist: String

  /** The timeout for all remote connections. */
  def connectionTimeout: FiniteDuration

  /** The expiration period for cached remote data. */
  def cacheExpiration: FiniteDuration

  /** The amount of time to allow for between remote calls. */
  def remoteCooldown: FiniteDuration

  /** The maximum number of entries to synchronize in a single pass. */
  def syncLimit: Int

  /** The optional path or URI to download the NLP sentence model from. */
  def sentenceModel: Option[String]

  /** The optional path or URI to download the NLP tokenizer model from. */
  def tokenizerModel: Option[String]

  /** The optional path or URI to download the NLP part of speech model from. */
  def partOfSpeechModel: Option[String]

  /** The optional path or URI to download the NLP name finder model from. */
  def nameFinderModel: Option[String]

/**
 * Definitions associated with configurations.
 */
object Configuration:

  import zio.{Has, URIO}

  /** The path or URL to use for storing persistant data. */
  val storage: URIO[Has[Configuration], String] = URIO.service map (_.storage)

  /** The designator that identifies the zeitgeist to use. */
  val zeitgeist: URIO[Has[Configuration], String] = URIO.service map (_.zeitgeist)

  /** The timeout for all remote connections. */
  val connectionTimeout: URIO[Has[Configuration], FiniteDuration] = URIO.service map (_.connectionTimeout)

  /** The expiration period for cached data. */
  val cacheExpiration: URIO[Has[Configuration], FiniteDuration] = URIO.service map (_.cacheExpiration)

  /** The amount of time to allow for between remote calls. */
  val remoteCooldown: URIO[Has[Configuration], FiniteDuration] = URIO.service map (_.remoteCooldown)

  /** The maximum number of entries to synchronize in a single pass. */
  val syncLimit: URIO[Has[Configuration], Int] = URIO.service map (_.syncLimit)

  /** The path or URL to download the NLP sentence model from. */
  val sentenceModel: URIO[Has[Configuration], Option[String]] = URIO.service map (_.sentenceModel)

  /** The optional path or URL to download the NLP tokenizer model from. */
  val tokenizerModel: URIO[Has[Configuration], Option[String]] = URIO.service map (_.tokenizerModel)

  /** The optional path or URI to download the NLP part of speech model from. */
  val partOfSpeechModel: URIO[Has[Configuration], Option[String]] = URIO.service map (_.partOfSpeechModel)

  /** The optional path or URI to download the NLP name finder model from. */
  val nameFinderModel: URIO[Has[Configuration], Option[String]] = URIO.service map (_.nameFinderModel)
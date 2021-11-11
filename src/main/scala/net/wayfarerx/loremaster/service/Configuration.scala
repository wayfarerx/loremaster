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

import java.net.URI

import scala.concurrent.duration.FiniteDuration

import zio.{Has, URIO}


/**
 * Definition of the configuration service.
 */
trait Configuration:

  /** The name of the application. */
  def applicationName: String

  /** The title of the application. */
  def applicationTitle: String

  /** The version of the application. */
  def applicationVersion: String

  /** The path or URI to use for storing persistent data. */
  def storage: String

  /** The designator that identifies the zeitgeist to use. */
  def zeitgeist: String

  /** The timeout for all remote connections. */
  def remoteConnectionTimeout: FiniteDuration

  /** The expiration period for cached remote data. */
  def remoteCacheExpiration: FiniteDuration

  /** The amount of time to allow for between remote calls. */
  def remoteCooldown: FiniteDuration

  /** The maximum number of entries to synchronize in a single pass. */
  def syncLimit: Int

  /** The optional URI to download the NLP sentence model from. */
  def sentenceModel: Option[URI]

  /** The optional URI to download the NLP tokenizer model from. */
  def tokenizerModel: Option[URI]

  /** The optional URI to download the NLP detokenizer dictionary from. */
  def detokenizerDictionary: Option[URI]

  /** The optional URI to download the NLP part of speech model from. */
  def partOfSpeechModel: Option[URI]

  /** The optional URI to download the NLP person name finder model from. */
  def personModel: Option[URI]

  /** The optional URI to download the NLP location name finder model from. */
  def locationModel: Option[URI]

  /** The optional URI to download the NLP organization name finder model from. */
  def organizationModel: Option[URI]

  /** The size constraint on the database cache. */
  def databaseCacheSize: Int

  /** The expiration period for the database cache. */
  def databaseCacheExpiration: FiniteDuration

/**
 * Definitions associated with configurations.
 */
object Configuration:

  /** The name of the application. */
  lazy val applicationName: URIO[Has[Configuration], String] =
    URIO.service map (_.applicationName)

  /** The title of the application. */
  lazy val applicationTitle: URIO[Has[Configuration], String] =
    URIO.service map (_.applicationTitle)

  /** The version of the application. */
  lazy val applicationVersion: URIO[Has[Configuration], String] =
    URIO.service map (_.applicationVersion)

  /** The path or URL to use for storing persistent data. */
  lazy val storage: URIO[Has[Configuration], String] =
    URIO.service map (_.storage)

  /** The designator that identifies the zeitgeist to use. */
  lazy val zeitgeist: URIO[Has[Configuration], String] =
    URIO.service map (_.zeitgeist)

  /** The timeout for all remote connections. */
  lazy val remoteConnectionTimeout: URIO[Has[Configuration], FiniteDuration] =
    URIO.service map (_.remoteConnectionTimeout)

  /** The expiration period for cached data. */
  lazy val remoteCacheExpiration: URIO[Has[Configuration], FiniteDuration] =
    URIO.service map (_.remoteCacheExpiration)

  /** The amount of time to allow for between remote calls. */
  lazy val remoteCooldown: URIO[Has[Configuration], FiniteDuration] =
    URIO.service map (_.remoteCooldown)

  /** The maximum number of entries to synchronize in a single pass. */
  lazy val syncLimit: URIO[Has[Configuration], Int] =
    URIO.service map (_.syncLimit)

  /** The optional URI to download the NLP sentence model from. */
  lazy val sentenceModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.sentenceModel)

  /** The optional URI to download the NLP tokenizer model from. */
  lazy val tokenizerModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.tokenizerModel)

  /** The optional URI to download the NLP detokenizer dictionary from. */
  lazy val detokenizerDictionary: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.detokenizerDictionary)

  /** The optional URI to download the NLP part of speech model from. */
  lazy val partOfSpeechModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.partOfSpeechModel)

  /** The optional URI to download the NLP person name finder model from. */
  lazy val personModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.personModel)

  /** The optional URI to download the NLP location name finder model from. */
  lazy val locationModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.locationModel)

  /** The optional URI to download the NLP organization name finder model from. */
  lazy val organizationModel: URIO[Has[Configuration], Option[URI]] =
    URIO.service map (_.organizationModel)

  /** The size constraint on the database cache. */
  lazy val databaseCacheSize: URIO[Has[Configuration], Int] =
    URIO.service map (_.databaseCacheSize)

  /** The expiration period for the database cache. */
  lazy val databaseCacheExpiration: URIO[Has[Configuration], FiniteDuration] =
    URIO.service map (_.databaseCacheExpiration)
/* Loremaster.scala
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
package main

import net.wayfarerx.loremaster.http.Http

import java.net.URI
import scala.concurrent.duration.*
import zio.{Has, RIO, Task, URIO, ZEnv, ZLayer}
import zio.clock.Clock
import zio.console.putStrLn
import zio.system.System

/**
 * The main entry point for the Loremaster application.
 */
object Loremaster extends zio.App :

  /** The environment that this program operates in. */
  private type Environment = ZEnv &
    Has[Configuration] &
    Has[Log.Factory] &
    Has[Storage] &
    Has[Http] &
    Has[Zeitgeist] &
    Has[Analysis] &
    Has[Library] &
    Has[Database] &
    Has[Synchronize]

  /** The title of this application. */
  private val ApplicationTitle = "Loremaster"

  /** The name of this application. */
  private val ApplicationName = ApplicationTitle.toLowerCase

  /** The version of this application. */
  private val ApplicationVersion = "0.x"

  /** The parser for configuration arguments. */
  private val parser = new OptionParser[Settings](ApplicationName) {
    head(ApplicationName, ApplicationVersion)
    opt[String]('s', "storage")
      .valueName("<path|url>")
      .text("The path or URL to use for storing data.")
      .action((storage, settings) => settings.copy(storage = storage))
    opt[String]('z', "zeitgeist")
      .valueName("<designator>")
      .text("The designator of the zeitgeist to use.")
      .action((zeitgeist, settings) => settings.copy(zeitgeist = zeitgeist))
    cmd("test")
      .text(s"  - Tests $ApplicationName.")
      .action((_, settings) => settings.copy(command = Command.Test))
    cmd("update")
      .text(s"  - Updates $ApplicationName against the remote zeitgeist.")
      .action((_, settings) => settings.copy(command = Command.Update))
  }

  /* The entry point for the the application. */
  override def run(args: List[String]): URIO[ZEnv, zio.ExitCode] = {
    for
      settings <- Task(parser.parse(args, Settings()))
      _ <- settings.fold(fail("Invalid command line arguments."))(apply)
    yield ()
  }.exitCode

  /**
   * Runs this application with the specified settings.
   *
   * @param settings The settings configured for this application.
   * @return An effect that attempts to run this application with the specified settings.
   */
  private def apply(settings: Settings): RIO[ZEnv, Unit] =
    val clock = ZLayer.service[Clock.Service]
    val system = ZLayer.service[System.Service]
    val configuration = ZLayer.succeed[Configuration](settings)
    val logFactory = Log.Factory.live
    val storage = system ++ configuration ++ logFactory >>> Storage.live
    val https = configuration ++ logFactory >>> Https.live
    val zeitgeist = clock ++ configuration ++ storage ++ https ++ logFactory >>> Zeitgeist.live
    val analysis = configuration ++ logFactory >>> Analysis.live
    val library = storage ++ logFactory >>> Library.live
    val database = configuration ++ storage >>> Database.live
    val synchronize = configuration ++ zeitgeist ++ analysis ++ library ++ logFactory >>> Synchronize.live
    settings.command.effect.provideCustomLayer(
      configuration ++
        logFactory ++
        storage ++
        https ++
        zeitgeist ++
        analysis ++
        library ++
        database ++
        synchronize
    )

  /**
   * Implementation of the configuration service.
   *
   * @param command                 The optional command that should be executed.
   * @param applicationName         The name of the application.
   * @param applicationTitle        The title of the application.
   * @param applicationVersion      The version of the application.
   * @param storage                 The path or URL to use for storing persistent data.
   * @param zeitgeist               The designator of the zeitgeist to fetch lore data from.
   * @param remoteConnectionTimeout The timeout for all remote connections.
   * @param remoteCacheExpiration   The expiration period for cached data.
   * @param remoteCooldown          The amount of time to allow for between remote calls.
   * @param syncLimit               The maximum number of entries to synchronize in a single pass.
   * @param sentenceModel           The optional URI to download the NLP sentence model from.
   * @param tokenizerModel          The optional URI to download the NLP tokenizer model from.
   * @param detokenizerDictionary   The optional URI to download the NLP detokenizer dictionary from.
   * @param partOfSpeechModel       The optional URI to download the NLP part of speech model from.
   * @param personModel             The optional URI to download the NLP person name finder model from.
   * @param locationModel           The optional URI to download the NLP location name finder model from.
   * @param organizationModel       The optional URI to download the NLP organization name finder model from.
   * @param databaseCacheSize       The size constraint on the database cache.
   * @param databaseCacheExpiration The expiration period for the database cache.
   */
  private case class Settings(
    command: Command = Command.Test,
    applicationName: String = ApplicationName,
    applicationTitle: String = ApplicationTitle,
    applicationVersion: String = ApplicationVersion,
    storage: String = s"~/.$ApplicationName",
    zeitgeist: String = Zeitgeist.TesImperialLibrary.Designator,
    remoteConnectionTimeout: FiniteDuration = 1.minute,
    remoteCacheExpiration: FiniteDuration = 1.day,
    remoteCooldown: FiniteDuration = 1.minute,
    syncLimit: Int = 100,
    sentenceModel: Option[URI] = None,
    tokenizerModel: Option[URI] = None,
    detokenizerDictionary: Option[URI] = None,
    partOfSpeechModel: Option[URI] = None,
    personModel: Option[URI] = None,
    locationModel: Option[URI] = None,
    organizationModel: Option[URI] = None,
    databaseCacheSize: Int = 100,
    databaseCacheExpiration: FiniteDuration = 1.day
  ) extends Configuration

  /**
   * The enumeration that defines the commands in this application.
   */
  private enum Command(val effect: RIO[Environment, Unit]):

    case Test extends Command(
      for
        storage <- Configuration.storage
        _ <- putStrLn(s"Storage: $storage")
      yield ()
    )

    /** The command that updates against the remote zeitgeist. */
    case Update extends Command(
      for
        zeitgeist <- Configuration.zeitgeist
        _ <- putStrLn(s"Updating from $zeitgeist.")
        _ <- Synchronize.sync()
      yield ()
    )

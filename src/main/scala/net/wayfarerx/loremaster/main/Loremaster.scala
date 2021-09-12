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

import scopt.OptionParser
import zio.{Has, RIO, Task, URIO, ZEnv, ZLayer}
import zio.clock.Clock
import zio.system.System
import service.*

/**
 * The main entry point for the Loremaster application.
 */
object Loremaster extends zio.App :

  /** The environment that this program operates in. */
  private type Environment = ZEnv &
    Has[Configuration] &
    Has[Log.Factory] &
    Has[Storage] &
    Has[Https] &
    Has[Zeitgeist] &
    Has[Analysis] &
    Has[Library] &
    Has[Synchronize]

  /** The name of this application. */
  private val Application = "loremaster"

  /** The version of this application. */
  private val Version = "0.x"

  /** The parser for configuration arguments. */
  private val parser = new OptionParser[Settings](Application) {
    head(Application, Version)
    opt[String]('s', "storage")
      .valueName("<path|url>")
      .text("The path or URL to use for storing data.")
      .action((storage, settings) => settings.copy(storage = storage))
    opt[String]('z', "zeitgeist")
      .valueName("<designator>")
      .text("The designator of the zeitgeist to use.")
      .action((zeitgeist, settings) => settings.copy(zeitgeist = zeitgeist))
    cmd("test")
      .text(s"  - Tests $Application.")
      .action((_, settings) => settings.copy(command = Command.Test))
    cmd("update")
      .text(s"  - Updates $Application against the remote zeitgeist.")
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
    val configuration = ZLayer.succeed[Configuration](settings)
    val logFactory = Log.Factory.live
    val storage = ZLayer.service[System.Service] ++ configuration ++ logFactory >>> Storage.live
    val https = configuration ++ logFactory >>> Https.live
    val zeitgeist = ZLayer.service[Clock.Service] ++ configuration ++ logFactory ++ storage ++ https >>> Zeitgeist.live
    val analysis = configuration ++ logFactory >>> Analysis.live
    val library = logFactory ++ storage >>> Library.live
    val synchronize = configuration ++ logFactory ++ zeitgeist ++ analysis ++ library >>> Synchronize.live
    settings.command.effect.provideCustomLayer(
      configuration ++
        logFactory ++
        storage ++
        https ++
        zeitgeist ++
        analysis ++
        library ++
        synchronize
    ) map (_ => ())

  import scala.concurrent.duration.*

  /**
   * Implementation of the configuration service.
   *
   * @param command           The optional command that should be executed.
   * @param storage           The path or URL to use for storing persistant data.
   * @param zeitgeist         The designator of the zeitgeist to fectch lore data from.
   * @param connectionTimeout The timeout for all remote connections.
   * @param cacheExpiration   The expiration period for cached data.
   * @param remoteCooldown    The amount of time to allow for between remote calls.
   * @param syncLimit         The maximum number of entries to synchronize in a single pass.
   * @param sentenceModel     The path or URI to download the NLP sentence model from.
   * @param tokenizerModel    The optional path or URI to download the NLP tokenizer model from.
   * @param partOfSpeechModel The optional path or URI to download the NLP part of speech model from.
   * @param nameFinderModel   The optional path or URI to download the NLP name finder model from.
   */
  private case class Settings(
    command: Command = Command.Test,
    storage: String = s"~/.$Application",
    zeitgeist: String = Zeitgeist.TesImperialLibrary.Designator,
    connectionTimeout: FiniteDuration = 1.minute,
    cacheExpiration: FiniteDuration = 1.day,
    remoteCooldown: FiniteDuration = 1.minute,
    syncLimit: Int = 100,
    sentenceModel: Option[String] = None,
    tokenizerModel: Option[String] = None,
    partOfSpeechModel: Option[String] = None,
    nameFinderModel: Option[String] = None
  ) extends Configuration

  import zio.console.*

  /**
   * The enumeration that defines the commands in this application.
   */
  private enum Command(val effect: RIO[Environment, Any]):

    case Test extends Command(
      for
        log <- Log.Factory("test")
        storage <- Configuration.storage
        _ <- log.debug(s"Storage: $storage")
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

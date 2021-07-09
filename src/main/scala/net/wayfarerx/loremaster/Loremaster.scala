package net.wayfarerx.loremaster

import scopt.OptionParser
import zio.{Has, RIO, Task, ZEnv, ZLayer}

/**
 * The main entry point for the Loremaster application.
 */
object Loremaster extends zio.App :

  /** The environment that this program operates in. */
  private type Environemnt = ZEnv &
    Has[Configuration] &
    Has[Storage] &
    Has[Library] &
    Has[Authority]

  /** The name of this application. */
  private val Application = "loremaster"

  /** The version of this application. */
  private val Version = "0.x"

  /** The parser for configuration arguments. */
  private val parser = new OptionParser[Settings](Application) {
    head(Application, Version)
    opt[String]('s', "storage")
      .valueName("<url>")
      .text("The path or URL to use for storing persistant data.")
      .action((s, c) => c.copy(storage = s))
    // Authority designation is currently disabled.
    // opt[String]('a', "authority")
    //   .valueName("<designator>")
    //   .text("The designator of the authority to use.")
    //   .action((a, c) => c.copy(authority = a))
    cmd("test")
      .text(s"Tests $Application.")
      .action((_, c) => c.copy(command = Command.Test))
  }

  /* The entry point for the the application. */
  override def run(args: List[String]) = {
    for
      settings <- Task(parser.parse(args, Settings()))
      result <- settings map apply getOrElse fail("Invalid command line arguments.")
    yield result
  }.exitCode

  /**
   * Runs this application with the specified settings.
   *
   * @param settings The settings configured for this application.
   * @return An effect that attempts to run this application with the specified settings.
   */
  private def apply(settings: Settings): RIO[ZEnv, Unit] =
    val configuration = ZLayer.succeed[Configuration](settings)
    val storage = configuration >>> Storage.live
    val library = storage >>> Library.live
    val authority = configuration >>> Authority.live
    settings.command.effect.provideCustomLayer(
      configuration ++
        storage ++
        library ++
        authority
    )

  /**
   * Implementation of the configuration service.
   *
   * @param command   The optional command that should be executed.
   * @param storage   The path or URL to use for storing persistant data.
   * @param authority The URL to fectch the index from.
   * @param frequency The frequency that the library is synchronized with the authority.
   */
  private case class Settings(
    command: Command = Command.Test,
    storage: String = s"~/.$Application",
    authority: String = Authority.TesImperialLibrary,
    frequency: String = "weekly"
  ) extends Configuration

  /**
   * The enumeration that defines the commands in this application.
   *
   * @param effect The effect that this command executes.
   */
  private enum Command(val effect: RIO[Environemnt, Unit]):

    case Test extends Command(
      for
        storage <- Configuration.storage
        _ <- zio.console.putStrLn(s"Storage: $storage")
      yield ()
    )

//case Sync extends Command(Reference.sync)

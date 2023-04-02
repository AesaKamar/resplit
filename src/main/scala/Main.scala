import cats.effect.{ExitCode, IO, IOApp}
import scopt.OParser

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    OParser
      .parse(
        parser = InputArgs.commandLineParser,
        args = args,
        init = InputArgs()
      )
      .fold(ifEmpty = IO(ExitCode.Error)) { parsedInputs =>
        Resplit
          .resplit(args = parsedInputs)
          .compile
          .drain
          .map(_ => ExitCode.Success)
      }
}

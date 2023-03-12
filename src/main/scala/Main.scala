import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}
import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.annotation.tailrec
import scala.util.matching.Regex

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    OParser
      .parse(parser = InputArgs.commandLineParser, args = args, init = InputArgs())
      .fold(ifEmpty = IO(ExitCode.Error)) { parsedInputs =>
        Resplit
          .resplit(parsedInputs)
          .compile
          .drain
          .map(_ => ExitCode.Success)
      }

}

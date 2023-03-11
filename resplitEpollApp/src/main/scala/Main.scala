import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import epollcat.EpollApp
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}
import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.annotation.tailrec
import scala.util.matching.Regex

object Main extends EpollApp {

  def run(args: List[String]): IO[ExitCode] =
    OParser
      .parse(Inputs.commandLineParser, args, Inputs())
      .fold(ifEmpty = IO(ExitCode.Error)) { parsedInputs =>
        Resplit.resplit(parsedInputs).compile.drain.map(_ => ExitCode.Success)
      }

}

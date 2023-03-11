import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import epollcat.EpollApp
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}
import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.annotation.tailrec
import scala.util.matching.Regex

case class Inputs(
    regexMatch: Regex = "".r,
    regexSub: Option[String] = None,
    digits: Int = 3,
    directory: Option[String] = None,
    file: Option[File] = None,
    suppressMatched: Boolean = false
)

object Inputs {
  private val builder: OParserBuilder[Inputs] = OParser.builder[Inputs]

  val commandLineParser: OParser[Unit, Inputs] = {
    import builder._
    OParser.sequence(
      programName("resplit"),
      note(
        """Splits a file based on a regex. split files will be prefixed by digits,
          |and named by the contents of the matched regular expression.
          |
          |Outputs names of files created to stdout
          |""".stripMargin
      ),
      arg[String]("regexMatch")
        .required()
        .text("A regular expression to split the file on ")
        .action((arg, conf) => conf.copy(regexMatch = arg.r)),
      arg[Option[String]]("regexSub")
        .optional()
        .text("A regular expression substitution expression to use to format the output filenames")
        .action((arg, conf) => conf.copy(regexSub = arg)),
      opt[Int]('n', "digits")
        .text("Number of digits to left-pad the split filenames with")
        .action((arg, conf) => conf.copy(digits = arg)),
      opt[String]('d', "directory")
        .text("Directory to write the split files into")
        .action((arg, conf) => conf.copy(directory = Some(arg))),
      opt[File]('f', "file")
        .text("Read from the specified file instead of stdin")
        .action((arg, conf) => conf.copy(file = Some(arg))),
      opt[Unit]("suppressMatched")
        .text(
          "Include the line that matched the regexMatch arg as the first line in the split files"
        )
        .action((_, conf) => conf.copy(suppressMatched = true)),
      help("help").text("prints this usage text")
    )
  }
}

object Main extends EpollApp {
  import cats.syntax.applicative._
  import scala.util.chaining._

  def run(args: List[String]): IO[ExitCode] =
    OParser
      .parse(Inputs.commandLineParser, args, Inputs())
      .fold(ifEmpty = IO(ExitCode.Error)) { input =>
        // Get from a file if provided, otherwise stdin
        input.file
          .fold(ifEmpty =
            fs2.io
              .stdin[IO](1024)
          ) {
            _.getPath
              .pipe(Path.apply)
              .pipe(fs2.io.file.Files[IO].readAll)
          }
          .through(fs2.text.utf8.decode)
          .through(fs2.text.lines)
          // Split files based on the matched regex
          .through { stream =>
            if (input.suppressMatched) stream.split(input.regexMatch.matches(_))
            else splitInclusive(stream)(input.regexMatch.matches(_))
          }
          .zipWithIndex
          // Create the directory to store outputs if it doesn't already exist
          .concurrently(Stream.eval(createDirectoryOrDoNothing(input.directory)))
          .evalTap { case (c, i) =>
            // get the new file name based on the matched regex
            inferPathFromFirstMatchedLineOfChunk(input, c, i)
              // Write out a new file stream
              .pipe(writeChunkToPathAndPrint(c, _))

          }
          .compile
          .drain
          .map(_ => ExitCode.Success)
      }

  /**
   * If the directory exists, return an empty effect
   * Otherwise, go create it!
   */
  def createDirectoryOrDoNothing(pathString: Option[String]) = pathString
    .map(Path.apply)
    .fold(ifEmpty = IO.unit) { dir =>
      fs2.io.file
        .Files[IO]
        .exists(dir)
        .flatMap {
          case true => IO.unit
          case false =>
            fs2.io.file
              .Files[IO]
              .createDirectory(dir)
        }
    }

  def writeChunkToPathAndPrint(c: Chunk[String], path: Path): IO[Unit] = fs2.Stream
    .chunk(c)
    .intersperse("\n")
    .append(fs2.Stream.emit("\n"))
    .through(fs2.text.utf8.encode)
    .through(fs2.io.file.Files[IO].writeAll(path)(_))
    .compile
    .drain
    .flatTap(_ => IO(Console.println(s"$path\t")))

  def inferPathFromFirstMatchedLineOfChunk(
      config: Inputs,
      c: Chunk[String],
      i: Long
  ): Path = {
    val fileContext: String = c.head
      .flatMap(config.regexMatch.findFirstIn(_))
      .map { matchedChars =>
        config.regexSub.fold(ifEmpty = matchedChars) { providedRegexSub =>
          try matchedChars.replaceFirst(config.regexMatch.regex, providedRegexSub)
          catch
            case _ =>
              Console.err.println(
                s"invalid regex: on $matchedChars for ${config.regexMatch.regex} with substitution $providedRegexSub"
              )
              ""
        }
      }
      .getOrElse("")
    val iii: String = leftPad(i.toString, config.digits, '0')
    config.directory
      .fold(ifEmpty = Path.apply(s"${iii}_$fileContext")) { dir =>
        Path.apply(s"$dir/${iii}_$fileContext")
      }

  }

  def leftPad(s: String, n: Int, c: Char): String = s.reverse
    .padTo(n, c)
    .reverse

  /** See [[fs2.Stream.split]]
    */
  def splitInclusive[F[_], O](
      t: Stream[F, O]
  )(f: O => Boolean): Stream[F, Chunk[O]] = {
    def go(buffer: Chunk[O], s: Stream[F, O]): Pull[F, Chunk[O], Unit] =
      s.pull.uncons.flatMap {
        case Some((hd, tl)) =>
          hd.indexWhere(f) match {
            case None => go(buffer ++ hd, tl)
            case Some(idx) =>
              val pfx = hd.take(idx)
              val b2  = buffer ++ pfx

              Pull.output1(b2) >> go(Chunk(hd.apply(idx)), tl.cons(hd.drop(idx + 1)))
          }
        case None =>
          if (buffer.nonEmpty) Pull.output1(buffer)
          else Pull.done
      }

    go(Chunk.empty, t).stream
  }
}

import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import epollcat.EpollApp
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}
import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.annotation.tailrec
import scala.util.matching.Regex

object Resplit {

  import cats.syntax.applicative.*
  import scala.util.chaining.*

  def resplit(input: Inputs): fs2.Stream[IO, (Path, Chunk[String])] =
    // Get from a file if provided, otherwise stdin
    input.inputFileInsteadOfStdin
      .fold(ifEmpty = fs2.io.stdin[IO](1024)) {
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
      .concurrently(Stream.eval(createDirectoryOrDoNothing(input.outputDirectory)))
      // get the new file name based on the matched regex
      .map { (c: Chunk[String], i: Long) => (inferPathFromFirstMatchedLineOfChunk(input, c, i), c) }
      // Write out a new file stream
      .evalTap { (p: Path, c: Chunk[String]) => writeChunkToPathAndPrint(c, p, input.silentMode) }

  /** If the directory exists, return an empty effect Otherwise, go create it!
    */
  def createDirectoryOrDoNothing(pathString: Option[String]): IO[Unit] = pathString
    .map(Path.apply)
    .fold(ifEmpty = IO.unit) { dir =>
      fs2.io.file
        .Files[IO]
        .exists(dir)
        .flatMap {
          if (_) IO.unit
          else
            fs2.io.file
              .Files[IO]
              .createDirectory(dir)
        }
    }

  def writeChunkToPathAndPrint(c: Chunk[String], path: Path, isSilent: Boolean = false): IO[Unit] =
    fs2.Stream
      .chunk(c)
      .intersperse("\n")
      .append(fs2.Stream.emit("\n"))
      .through(fs2.text.utf8.encode)
      .through(fs2.io.file.Files[IO].writeAll(path)(_))
      .compile
      .drain
      .flatTap { _ =>
        if (isSilent) IO.unit
        else IO(Console.println(s"$path\t"))
      }

  // TODO this is technically unsafe because is has a console printing effect in it
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
          catch {

            case _ =>
              if (!config.silentMode)
                Console.err.println(
                  s"invalid regex: on $matchedChars for ${config.regexMatch.regex} with substitution $providedRegexSub"
                )
              ""
          }
        }
      }
      .getOrElse("")
    val iii: String = leftPad(i.toString, config.filenamePaddingDigits, '0')
    config.outputDirectory
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

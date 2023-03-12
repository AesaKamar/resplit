import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}
import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.annotation.tailrec
import scala.util.matching.Regex

object Resplit {

  import cats.syntax.applicative.*
  import scala.util.chaining.*

  def resplit(args: InputArgs): fs2.Stream[IO, (Path, Chunk[String])] =
    args.inputFileInsteadOfStdin
      .fold(ifEmpty = fs2.io.stdin[IO](bufSize = 1024)) {
        _.getPath
          .pipe(Path.apply)
          .pipe(fs2.io.file.Files[IO].readAll)
      }
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .through { inputStream =>
        if (args.suppressMatched) inputStream.split(thisLine => args.regexToMatch.matches(thisLine))
        else splitInclusive(inputStream)(thisLine => args.regexToMatch.matches(thisLine))
      }
      .zipWithIndex
      .concurrently(createDirectoryOrDoNothing(args.outputDirectory).pipe(Stream.eval))
      .map { (chunkOfLines, chunkNumber) =>
        (inferPathFromFirstMatchedLineOfChunk(args, chunkOfLines, chunkNumber), chunkOfLines)
      }
      .evalTap { (path, chunkOfLines) =>
        writeChunkToPathAndPrint(chunkOfLines, path, args.silentMode)
      }

  /** If the directory exists, return an empty effect Otherwise, go create it!
    */
  def createDirectoryOrDoNothing(outputDirectory: Option[File]): IO[Unit] = outputDirectory
    .map(_.getPath)
    .map(Path.apply)
    .fold(ifEmpty = IO.unit) { dir =>
      fs2.io.file
        .Files[IO]
        .exists(dir)
        .flatMap { exists =>
          if (exists) IO.unit
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

  // TODO this is technically impure because is has a console printing effect in it
  def inferPathFromFirstMatchedLineOfChunk(
      args: InputArgs,
      c: Chunk[String],
      i: Long
  ): Path = {
    val fileContext: String = c.head
      .flatMap(args.regexToMatch.findFirstIn(_))
      .map { matchedChars =>
        args.regexToSub.fold(ifEmpty = matchedChars) { providedRegexSub =>
          try matchedChars.replaceFirst(args.regexToMatch.regex, providedRegexSub)
          catch
            case _ =>
              if (!args.silentMode)
                s"invalid regex: on $matchedChars for ${args.regexToMatch.regex} with substitution $providedRegexSub"
                  .pipe(Console.err.println)
              ""
        }
      }
      .getOrElse("")
    val iii: String = leftPad(i.toString, args.filenamePaddingDigits, '0')
    args.outputDirectory
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

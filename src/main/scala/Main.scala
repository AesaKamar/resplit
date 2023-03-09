import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import epollcat.EpollApp
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}

import scala.annotation.tailrec
import scala.util.matching.Regex

object Main extends EpollApp {
  import cats.syntax.applicative._

  def run(args: List[String]): IO[ExitCode] =
    val regexToMatch: Regex = args.head.r
    val regexSub            = args.drop(1).headOption
    fs2.io
      .stdin[IO](1024)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .through(splitInclusive(_)(regexToMatch.matches(_)))
      .zipWithIndex
      .evalTap { case (c, i) =>
        val path = inferPathFromFirstMatchedLineOfChunk(
          regex = regexToMatch,
          regexSub = regexSub,
          c = c,
          i = i
        )
        fs2.Stream
          .chunk(c)
          .intersperse("\n")
          .append(fs2.Stream.emit("\n"))
          .through(fs2.text.utf8.encode)
          .through(fs2.io.file.Files[IO].writeAll(path)(_))
          .compile
          .drain
          .flatTap(_ => IO(Console.println(s"$path\t")))
      }
      .compile
      .drain
      .map(_ => ExitCode.Success)

  def inferPathFromFirstMatchedLineOfChunk(
      regex: Regex,
      regexSub: Option[String],
      c: Chunk[String],
      i: Long
  ): Path = {
    val fileContext: String = c.head
      .flatMap(regex.findFirstIn)
      .map { matchedChars =>
        regexSub.fold(ifEmpty = matchedChars) { providedRegexSub =>
          try matchedChars.replaceFirst(regex.regex, providedRegexSub)
          catch
            case e: Throwable =>
              Console.err.println(s"invalid regex: on $matchedChars for ${regex.regex} with substitution $providedRegexSub")
              ""
        }
      }
      .getOrElse("")
    val iii: String = leftPad(i.toString, 3, '0')
    Path.apply(s"${iii}_$fileContext")
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

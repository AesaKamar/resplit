import cats.Applicative
import cats.effect.{ExitCode, IO, IOApp}
import epollcat.EpollApp
import fs2.io.file.Path
import fs2.{Chunk, Pull, Stream}

object Main extends EpollApp {
  import cats.syntax.applicative._

  def leftPad(s: String, n: Int, c: Char): String = s.reverse
    .padTo(n, c)
    .reverse

  def run(args: List[String]): IO[ExitCode] =
    val regexToMatch = args.head.r
    fs2.io
      .stdin[IO](1024)
      .through(fs2.text.utf8.decode)
      .through(fs2.text.lines)
      .through(splitInclusive(_)(regexToMatch.matches(_)))
      .zipWithIndex
      .evalTap { case (c, i) =>
        val fileContext = c.head
          .flatMap(regexToMatch.findFirstIn)
          .getOrElse("")
        val iii  = leftPad(i.toString, 3, '0')
        val path = Path.apply(s"${iii}_$fileContext")
        pprint.log(path)
        fs2.Stream
          .chunk(c)
          .intersperse("\n")
          .append(fs2.Stream.emit("\n"))
          .through(fs2.text.utf8.encode)
          .through(fs2.io.file.Files[IO].writeAll(path)(_))
          .compile
          .drain
      }
      .compile
      .drain
      .map(_ => ExitCode.Success)

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

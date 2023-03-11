import cats.Id
import munit.FunSuite
import cats.effect.IO
import fs2.{Chunk, Pure}
import fs2.io.file.Path

import java.io.File
import scala.util.chaining.*

class ResplitTest extends munit.CatsEffectSuite {

  test("splitInclusive") {
    val value: fs2.Stream[Pure, Int] =
      fs2.Stream.range(0, 10)

    val predicate: Int => Boolean =
      _ % 4 == 0

    val split: fs2.Stream[Pure, Chunk[Int]] =
      Resplit.splitInclusive(value)(predicate)

    val allBeginWithPredicate: Boolean = split
      .forall(_.head.forall(predicate))
      .compile
      .toList
      .head

    assert(allBeginWithPredicate)
  }

  test("leftPad") {
    assertEquals(Resplit.leftPad("1", 5, '0'), "00001")
  }

  test("inferPathFromFirstMatchedLineOfChunk") {
    val inputs = Inputs(regexMatch = "(dog)\\d".r, regexSub = Some("$1"), filenamePaddingDigits = 3)

    val filePath = Resplit.inferPathFromFirstMatchedLineOfChunk(
      inputs,
      Chunk(
        "dog1",
        "cat4",
        "cat5",
        "cat6"
      ),
      0
    )

    assertEquals(filePath, Path("000_dog"))
  }

  test("resplit") {
    for {
      output <- Resplit
        .resplit(
          Inputs(
            regexMatch = "dog\\d".r,
            regexSub = None,
            filenamePaddingDigits = 3,
            outputDirectory = Some("tmp"),
            inputFileInsteadOfStdin = Some(new File("testfile")),
            suppressMatched = false,
            silentMode = true
          )
        )
        .compile
        .toList
      expectedOutput = List(
        (Path("tmp/000_"), Chunk("cat1", "cat2", "cat3")),
        (Path("tmp/001_dog1"), Chunk("dog1", "cat4", "cat5", "cat6")),
        (Path("tmp/002_dog2"), Chunk("dog2", "cat7", "cat8", "cat9"))
      )
    } yield {

      assertEquals(output, expectedOutput)
    }
  }
}

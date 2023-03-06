import cats.Id
import munit.FunSuite
import cats.effect.IO
import fs2.Chunk
import fs2.io.file.Path

import scala.util.chaining.*
import cats.effect.unsafe.implicits.*

class MainTest extends FunSuite {

  test("splitInclusive") {
    val value = fs2.Stream.range(0, 10)

    val predicate: Int => Boolean = _ % 4 == 0

    val split = Main.splitInclusive(value)(predicate)

    val allBeginWithPredicate = split
      .forall(_.head.map(predicate).getOrElse(true))
      .compile
      .toList
      .head

    assert(allBeginWithPredicate)
  }

}

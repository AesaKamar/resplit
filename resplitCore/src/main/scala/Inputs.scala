import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.util.matching.Regex

case class Inputs(
    regexMatch: Regex = "".r,
    regexSub: Option[String] = None,
    filenamePaddingDigits: Int = 3,
    outputDirectory: Option[String] = None,
    inputFileInsteadOfStdin: Option[File] = None,
    suppressMatched: Boolean = false,
    silentMode: Boolean = false
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
        .action((arg, conf) => conf.copy(filenamePaddingDigits = arg)),
      opt[String]('d', "directory")
        .text("Directory to write the split files into")
        .action((arg, conf) => conf.copy(outputDirectory = Some(arg))),
      opt[File]('f', "file")
        .text("Read from the specified file instead of stdin")
        .action((arg, conf) => conf.copy(inputFileInsteadOfStdin = Some(arg))),
      opt[Unit]("suppressMatched")
        .text(
          "Include the line that matched the regexMatch arg as the first line in the split files"
        )
        .action((_, conf) => conf.copy(suppressMatched = true)),
      opt[Unit]('s', "quiet")
        .text("Quiet")
        .action((_, conf) => conf.copy(silentMode = true)),
      help("help").text("prints this usage text")
    )
  }
}

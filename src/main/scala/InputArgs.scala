import scopt.{OParser, OParserBuilder}

import java.io.File
import scala.util.matching.Regex

case class InputArgs(
    regexToMatch: Regex = "".r,
    regexToSub: Option[String] = None,
    filenamePaddingDigits: Int = 3,
    outputDirectory: Option[File] = None,
    inputFileInsteadOfStdin: Option[File] = None,
    suppressMatched: Boolean = false,
    silentMode: Boolean = false,
    elideEmptyFiles: Boolean = false
)

/** Use [[OParserBuilder]] API to specify how to take input args from the
  * command line and make them user friendly
  */
object InputArgs {
  import scala.util.chaining._
  private val builder: OParserBuilder[InputArgs] = OParser.builder[InputArgs]

  val commandLineParser: OParser[Unit, InputArgs] = {
    import builder._
    OParser.sequence(
      programName("resplit"),
      note(
        """Splits input based on a regex. Split files will be prefixed by digits,
          |and named by the contents of the matched regular expression.
          |
          |Outputs names of files created to stdout
          |""".stripMargin
      ),
      arg[String]("regexToMatch")
        .required()
        .text("A regular expression to split the file on ")
        .action((arg, conf) => conf.copy(regexToMatch = arg.r)),
      arg[Option[String]]("regexToSub")
        .optional()
        .text(
          "A regular expression substitution expression to use to format the output filenames"
        )
        .action((arg, conf) => conf.copy(regexToSub = arg)),
      opt[Int]('n', "digits")
        .optional()
        .text("Number of digits to left-pad the split filenames with")
        .validate(n =>
          (n > 0 && n <= 128)
            .pipe(Either.cond(_, (), "Not a valid number of digits"))
        )
        .action((arg, conf) => conf.copy(filenamePaddingDigits = arg)),
      opt[File]('d', "directory")
        .text("Directory to write the split files into")
        .optional()
        .action((arg, conf) => conf.copy(outputDirectory = Some(arg))),
      opt[File]('f', "file")
        .text("Read from the specified file instead of stdin")
        .optional()
        .validate(_.isFile.pipe(Either.cond(_, (), "Not a valid file")))
        .action((arg, conf) => conf.copy(inputFileInsteadOfStdin = Some(arg))),
      opt[Unit]("suppressMatched")
        .optional()
        .text(
          "Include the line that matched the regexMatch arg as the first line in the split files"
        )
        .action((_, conf) => conf.copy(suppressMatched = true)),
      opt[Unit]('s', "quiet")
        .optional()
        .text("Quiet")
        .action((_, conf) => conf.copy(silentMode = true)),
      opt[Unit]('z', "elide-empty-files")
        .optional()
        .text("Remove empty output files")
        .action((_, conf) => conf.copy(elideEmptyFiles = true)),
      help("help").text("prints this usage text")
    )
  }
}

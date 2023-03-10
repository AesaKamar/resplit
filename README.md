## resplit
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A command line utility for splitting files based on a regular expression

A reimplementation of the gnu command line utility `csplit` https://man7.org/linux/man-pages/man1/csplit.1.html



### Usage
```sh
> resplit --help
Usage: resplit [options] regexMatch [regexSub]

Splits a file based on a regex. split files will be prefixed by digits, and named by the contents of the matched regular expression
  --help                   prints this usage text
  regexMatch               A regular expression to split the file on
  regexSub                 A regular expression substitution expression to use to format the output filenames
  -n, --digits <value>     Number of digits to left-pad the split filenames with
  -d, --directory <value>  Directory to write the split files into
  -f, --file <value>       Read from the specified file instead of stdin
  --suppressMatched        Include the line that matched the regexMatch arg as the firt line in the split files
```


### Installation 

# `resplit`
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

A command line utility for splitting files based on a regular expression

-- reimplementation of gnu `csplit` https://man7.org/linux/man-pages/man1/csplit.1.html


Built with [ScalaNative](https://scala-native.org/en/stable/), [fs2](https://github.com/typelevel/fs2) and [cats](https://github.com/typelevel/cats)!

## Demo
Streaming "The Adventures of Sherlock Holmes" with `wget` and spliting by chapter regex into unique files


![Resplit_Mov_AdobeExpress](https://user-images.githubusercontent.com/4334491/224520721-9500f632-12a6-41f0-859a-d5f84b4d6b01.gif)



## Usage
```sh
> resplit --help
Usage: resplit [options] regexToMatch [regexToSub]

Splits a file based on a regex. split files will be prefixed by digits,
and named by the contents of the matched regular expression.

Outputs names of files created to stdout

  regexToMatch             A regular expression to split the file on
  regexToSub               A regular expression substitution expression to use to format the output filenames
  -n, --digits <value>     Number of digits to left-pad the split filenames with
  -d, --directory <value>  Directory to write the split files into
  -f, --file <value>       Read from the specified file instead of stdin
  --suppressMatched        Include the line that matched the regexMatch arg as the first line in the split files
  -s, --quiet              Quiet
  -z, --elide-empty-files  Remove empty output files
  --help                   prints this usage text
```

## Installation
- Download the latest release for your target platform 
  - ```sh
    wget https://github.com/aesakamar/resplit/releases/download/v0.1.1/resplit-macos-latest
    ```
- Grant executable permissions on the downloaded file 
  - ```
    chmod +x resplit-macos-latest
    ```
- Move the executable to a place accessible onyour $PATH
  - ```
    mv resplit-macos-latest ~/bin/resplit
    ```
## Examples

#### Input: 
--- 
```
cat1
cat2
cat3
dog1
cat4
cat5
cat6
dog2
cat7
cat8
cat9
```

```
> cat testfile | resplit '(dog)\d' '$1'
```

#### Output: 
--- 
`000_`
```
cat1
cat2
cat3
```

`001_dog`
```
dog1
cat4
cat5
cat6
```

`002_dog`
```
dog2
cat7
cat8
cat9
```

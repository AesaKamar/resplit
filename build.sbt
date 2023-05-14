name := "resplit"

scalaVersion := "3.2.2"

// This tells our project
enablePlugins(ScalaNativePlugin)

// defaults set with common options shown
nativeConfig ~= { configuration =>
  configuration
    // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
    .withLTO(scala.scalanative.build.LTO.none)
    // https://scala-native.org/en/stable/user/sbt.html#compilation-modes
    .withMode(scala.scalanative.build.Mode.debug)
    // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
    .withGC(scala.scalanative.build.GC.commix)
}

libraryDependencies ++= Seq(
  "co.fs2"           %%% "fs2-core"          % "3.7.0",
  "co.fs2"           %%% "fs2-io"            % "3.7.0",
  "org.typelevel"    %%% "cats-effect"       % "3.4.8",
  "org.typelevel"    %%% "cats-core"         % "2.9.0",
  "com.github.scopt" %%% "scopt"             % "4.1.0",
  "com.lihaoyi"      %%% "pprint"            % "0.8.1",
  "org.typelevel"    %%% "munit-cats-effect" % "2.0.0-M3" % Test
)

// I often declare and do not use things while developing
tpolecatExcludeOptions ++= ScalacOptions.warnUnusedOptions

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

name := "resplit"

scalaVersion := "3.2.2"

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

lazy val resplitCore: Project = project
  .enablePlugins(ScalaNativePlugin)
  .settings(
    name         := "resplitCore",
    scalaVersion := "3.2.2",
    // defaults set with common options shown
    nativeConfig ~= { c =>
      c
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withLTO(scala.scalanative.build.LTO.none)
        // https://scala-native.org/en/stable/user/sbt.html#compilation-modes
        .withMode(scala.scalanative.build.Mode.debug)
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withGC(scala.scalanative.build.GC.commix)
    },
    libraryDependencies ++= Seq(
      "co.fs2"           %%% "fs2-core"          % "3.6.1",
      "co.fs2"           %%% "fs2-scodec"        % "3.6.1",
      "com.armanbilge"   %%% "epollcat"          % "0.1.4",
      "co.fs2"           %%% "fs2-io"            % "3.6.1",
      "org.typelevel"    %%% "cats-effect"       % "3.4.8",
      "org.typelevel"    %%% "cats-core"         % "2.9.0",
      "com.lihaoyi"      %%% "pprint"            % "0.8.1",
      "com.github.scopt" %%% "scopt"             % "4.1.0",
      "org.typelevel"    %%% "munit-cats-effect" % "2.0.0-M3" % Test,
      "org.scalameta"    %%% "munit"             % "1.0.0-M7" % Test
    ),
    tpolecatExcludeOptions ++= Set(
      ScalacOptions.warnUnusedImports,
      ScalacOptions.warnUnusedImplicits
    )
  )

/** Building for mac an linux, backed by EPoll IO Runtime
 */
lazy val resplitEpollApp: Project = project
  .enablePlugins(ScalaNativePlugin)
  .settings(
    name := "resplitEpollApp",
    // defaults set with common options shown
    scalaVersion := "3.2.2",
    nativeConfig ~= { c =>
      c
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withLTO(scala.scalanative.build.LTO.none)
        // https://scala-native.org/en/stable/user/sbt.html#compilation-modes
        .withMode(scala.scalanative.build.Mode.releaseSize)
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withGC(scala.scalanative.build.GC.commix)
    },
    Compile / mainClass := Some("Main")
  )
  .dependsOn(resplitCore)

/** Building for windows for which there is not an EPoll Binding see:
 * https://github.com/armanbilge/epollcat#windows-support
 */
lazy val resplitIOApp: Project = project
  .enablePlugins(ScalaNativePlugin)
  .settings(
    name         := "resplitIOApp",
    scalaVersion := "3.2.2",
    // defaults set with common options shown
    nativeConfig ~= { c =>
      c
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withLTO(scala.scalanative.build.LTO.none)
        // https://scala-native.org/en/stable/user/sbt.html#compilation-modes
        .withMode(scala.scalanative.build.Mode.releaseSize)
        // https://scala-native.org/en/stable/user/sbt.html#garbage-collectors
        .withGC(scala.scalanative.build.GC.commix)
    },
    Compile / mainClass := Some("Main")
  )
  .dependsOn(resplitCore)

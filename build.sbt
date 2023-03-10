scalaVersion := "3.2.2"

enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// import to add Scala Native options
import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none)     // thin
    .withMode(Mode.releaseSize) // releaseFast
    .withGC(GC.immix)     // commix
}
libraryDependencies ++= Seq(
  "co.fs2"           %%% "fs2-core"    % "3.6.1",
  "co.fs2"           %%% "fs2-scodec"  % "3.6.1",
  "com.armanbilge"   %%% "epollcat"    % "0.1.4",
  "co.fs2"           %%% "fs2-io"      % "3.6.1",
  "org.typelevel"    %%% "cats-effect" % "3.4.8",
  "org.typelevel"    %%% "cats-core"   % "2.9.0",
  "com.lihaoyi"      %%% "pprint"      % "0.8.1",
  "com.github.scopt" %%% "scopt"       % "4.1.0"
)
libraryDependencies ++= Seq(
  "org.scalameta" %%% "munit" % "1.0.0-M7" % Test
)
tpolecatExcludeOptions ++= Set(
  ScalacOptions.warnUnusedImports,
  ScalacOptions.warnUnusedImplicits
)

Compile / mainClass := Some("Main")

name := "templates"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
  cache,
  specs2 % Test,
  "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
  "org.scalaz" %% "scalaz-effect" % "7.1.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.1.6"
)

// see https://github.com/scalatra/scalatra/pull/325
dependencyOverrides := Set(
  "org.scala-lang" %  "scala-library"  % scalaVersion.value,
  "org.scala-lang" %  "scala-reflect"  % scalaVersion.value,
  "org.scala-lang" %  "scala-compiler" % scalaVersion.value,
  "org.scalaz" %% "scalaz-effect" % "7.0.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.0.6",
  "org.webjars" %% "webjars-play" % "2.3.0-3",
  "org.webjars.npm" % "react" % "0.14.3",
  "org.webjars.npm" % "react-dom" % "0.14.3",
  "org.webjars.npm" % "jquery" % "2.1.4",
  "com.lihaoyi" %% "ammonite-ops" % "0.5.2"
)

javaOptions in Test += "-Dconfig.file=test/template/test.conf"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb)

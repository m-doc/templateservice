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
  "org.scala-lang" %  "scala-compiler" % scalaVersion.value
)

javaOptions in Test += "-Dconfig.file=test/template/test.conf"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

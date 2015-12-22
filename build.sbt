name := "templates"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  cache,
  "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
  "org.scalaz" %% "scalaz-effect" % "7.0.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.0.6",
  "org.webjars" %% "webjars-play" % "2.3.0-3",
  "org.webjars.npm" % "react" % "0.14.3",
  "org.webjars.npm" % "react-dom" % "0.14.3"
)

javaOptions in Test += "-Dconfig.file=test/template/test.conf"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtWeb)

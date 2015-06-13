import sbt.Keys._
import sbt.Project.projectToRef

import sbt.Project.projectToRef

lazy val clients = Seq(client)
lazy val scalaV = "2.11.6"

lazy val server = (project in file("server")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := clients,
  pipelineStages := Seq(scalaJSProd),
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
    "org.scalaz" %% "scalaz-effect" % "7.0.6",
    "com.vmunier" %% "play-scalajs-scripts" % "0.2.2",
    "org.webjars" % "jquery" % "1.11.1")
  )
  .enablePlugins(PlayScala).
  aggregate(clients.map(projectToRef): _*).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  sourceMapsDirectories += sharedJs.base / "..",
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.8.0"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSPlay).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay).
  jsSettings(sourceMapsBase := baseDirectory.value / "..")

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the Play project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value

/*
lazy val commonSettings = Seq(
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.6"
)

lazy val server = (project in file("server"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      cache,
      "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
      "org.scalaz" %% "scalaz-effect" % "7.0.6"),
    name := "templates-server"
  )
  .dependsOn(sharedJvm)

lazy val client = (project in file("shared"))
  .settings(commonSettings: _*)
  .settings(
    name := "templates-client",
    sourceMapsDirectories += sharedJs.base / ".."
  )
  .dependsOn(sharedJs)
  .enablePlugins(ScalaJSPlugin, ScalaJSPlay)

lazy val shared = (project in file("shared"))
  .settings(commonSettings: _*)
  .settings(
    name := "templates-shared",
    sourceMapsBase := baseDirectory.value / ".."
  )
  //.jsConfigure(_ enablePlugins ScalaJSPlay)
  .enablePlugins(ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val root = (project in file("."))
  .settings(
    name := "templates"
  )
  .aggregate(server, client)
  .enablePlugins(PlayScala)
*/


name := "templates"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "org.scalatra.scalate" %% "scalate-core" % "1.7.1"
)



lazy val root = (project in file(".")).enablePlugins(PlayScala)


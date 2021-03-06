name := """simple-screen-share"""
organization := "skabele"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.8"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.17"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.17"
libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.8"
libraryDependencies += "com.beachape" %% "enumeratum-play-json" % "1.5.8"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "skabele.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "skabele.binders._"

import AssemblyKeys._

assemblySettings

mainClass in assembly := Some("play.core.server.ProdServerStart")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

mergeStrategy in assembly := {
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (mergeStrategy in assembly).value
    oldStrategy(x)
}

jarName in assembly := "simple-screen-share.jar"

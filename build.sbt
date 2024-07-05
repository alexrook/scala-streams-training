ThisBuild / useCoursier := false // https://www.scala-sbt.org/1.x/docs/sbt-1.3-Release-Notes.html#Library+management+with+Coursier

ThisBuild / version := "0.0.1"

ThisBuild / scalaVersion := "2.13.14"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

lazy val compileSettings =
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-language:existentials",
    "-language:higherKinds",
    "-Ywarn-unused",
    "-Ywarn-dead-code",
    "-Ymacro-annotations"
  )

lazy val akkaLibs = {
  val akkaVersion = "2.9.4"
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion
  )
}

lazy val scalaTestLibs = {
  lazy val scalatestVersion = "3.2.17"
  Seq(
    "org.scalatest" %% "scalatest" % scalatestVersion % Test
  )
}

lazy val loggingLibs = {
  val slf4jV = "2.0.11"
  val logbackVersion =
    "1.3.6" // 1.4.0 does not support Java 8, so 1.3.X is a latest version for Java 8
  Seq(
    "org.slf4j" % "slf4j-api" % slf4jV,
    "ch.qos.logback" % "logback-classic" % logbackVersion
  )
}

lazy val circeLibs = {
  val V = "0.14.1"
  Seq(
    "io.circe" %% "circe-core" % V,
    "io.circe" %% "circe-generic" % V,
    "io.circe" %% "circe-parser" % V,
    "io.circe" %% "circe-generic-extras" % V
  )
}

lazy val soptLibs: Seq[ModuleID] = {
  val V = "4.1.0"
  Seq(
    "com.github.scopt" %% "scopt" % V
  )
}

libraryDependencies ++= akkaLibs

import sbt._
import Keys._
import PlayProject._

object BuildSettings {
  val buildOrganization = "ar.edu.itba.it.scala.experimental"
  val buildVersion = "1.0-SNAPSHOT"
  val buildScalaVersion = "2.9.1"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion)
}

object Resolvers {
  val typesafeRepo = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

  val eligosourceReleasesRepo =
    "Eligosource Releases Repo" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-releases/"
  val eligosourceSnapshotsRepo =
    "Eligosource Snapshots Repo" at "http://repo.eligotech.com/nexus/content/repositories/eligosource-snapshots/"
}

object Versions {
  val Akka = "2.0.3"
  val Spring = "3.1.0.RELEASE"
}

object Dependencies {
  import Versions._

  // compile dependencies
  lazy val akkaActor = "com.typesafe.akka" % "akka-actor" % Akka % "compile"
  lazy val jsr311 = "javax.ws.rs" % "jsr311-api" % "1.1.1" % "compile"
  lazy val eventsourced = "org.eligosource" % "eventsourced_2.9.2" % "0.4.1" % "compile"
  lazy val scalaStm = "org.scala-tools" %% "scala-stm" % "0.5" % "compile"
  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4" % "compile"
  lazy val springWeb = "org.springframework" % "spring-web" % Spring % "compile"

  // runtime dependencies
  lazy val configgy = "net.lag" % "configgy" % "2.0.0" % "runtime"

  // test dependencies
  lazy val scalatest = "org.scalatest" %% "scalatest" % "1.8" % "test"
}

object ApplicationBuild extends Build {
  import BuildSettings._
  import Resolvers._
  import Dependencies._
  val appName = "library"

  val main = PlayProject(appName, mainLang = SCALA, settings = buildSettings ++ Seq(
    resolvers := Seq(typesafeRepo, eligosourceReleasesRepo, eligosourceSnapshotsRepo),
    // compile dependencies (backend)
    libraryDependencies ++= Seq(akkaActor, scalaStm, eventsourced, scalaz),
    // container dependencies
    // runtime dependencies
    libraryDependencies ++= Seq(configgy),
    // test dependencies
    libraryDependencies ++= Seq(scalatest),
    routesImport += "domain._"))
}
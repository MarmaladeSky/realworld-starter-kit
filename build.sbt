import complete.DefaultParsers._
import sbtrelease.ReleaseStateTransformations._

val Http4sVersion = "0.23.30"
val Http4sBlazeVersion = "0.23.17"
val CatsVersion = "2.13.0"
val CatsEffectVersion = "3.6.0"
val CirceVersion = "0.14.12"
val CirceOpticsVersion = "0.15.0"
val CirceGenericVersion = "0.14.12"
val CirceGenericExtrasVersion = "0.14.4"
val Fs2Version = "3.12.0"
val MunitVersion = "1.1.0"
val LogbackVersion = "1.5.18"
val MunitCatsEffectVersion = "2.1.0"
val SlickVersion = "3.6.0"
val SlickPgVersion = "0.23.0"
val SvmSubsVersion = "20.2.0"
val JwtScalaVersion = "10.0.4"
val SlugifyVersion = "3.0.7"
val ScalaLoggingVersion = "3.9.5"
val Log4CatsVersion = "2.7.0"
val PureConfigVersion = "0.17.8"
val MockitoScalaVersion = "1.17.37"
val Slf4jApiVersion = "2.0.17"
val ShapelessVersion = "2.3.10"
val TypelevelKindProjectorVersion = "0.13.3"
val TypesafeConfigVersion = "1.4.3"
val TypelevelCaseInsensitiveVersion = "1.4.2"
val BetterMonadicForVersion = "0.3.1"
val JAnsiVersion = "2.4.1"

lazy val root = (project in file("."))
  .settings(
    organization := "com.marmaladesky",
    name := "condoit",
    scalaVersion := "2.13.14",
    scalacOptions ++= Seq(
      "-Ymacro-annotations",
      """-Wconf:src=.*gen\/Tables\.scala:silent"""
    ),
    libraryDependencies ++= Seq(
      // Core
      "org.typelevel" %% "cats-core" % CatsVersion,
      "org.typelevel" %% "cats-kernel" % CatsVersion,
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.typelevel" %% "cats-effect-kernel" % CatsEffectVersion,
      "org.http4s" %% "http4s-core" % Http4sVersion,
      "org.http4s" %% "http4s-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sBlazeVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceGenericVersion,
      "io.circe" %% "circe-generic-extras" % CirceGenericExtrasVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Runtime,
      "co.fs2" %% "fs2-core" % Fs2Version,
      "org.typelevel" %% "case-insensitive" % TypelevelCaseInsensitiveVersion,

      // Third-party misc
      "com.github.jwt-scala" %% "jwt-core" % JwtScalaVersion,
      "com.github.jwt-scala" %% "jwt-circe" % JwtScalaVersion,
      "com.github.slugify" % "slugify" % SlugifyVersion,

      // Slick
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion % Runtime,
      "com.github.tminglei" %% "slick-pg_core" % SlickPgVersion,
      "com.github.tminglei" %% "slick-pg" % SlickPgVersion,
      "com.typesafe.slick" %% "slick-codegen" % SlickVersion,

      // Logging
      "org.typelevel" %% "log4cats-core" % Log4CatsVersion,
      "org.typelevel" %% "log4cats-slf4j" % Log4CatsVersion,
      "org.slf4j" % "slf4j-api" % Slf4jApiVersion % Runtime,
      "org.fusesource.jansi" % "jansi" % JAnsiVersion % Runtime,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,

      // Config
      "com.typesafe" % "config" % TypesafeConfigVersion,
      "com.github.pureconfig" %% "pureconfig-core" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic-base" % PureConfigVersion,

      // Test
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect" % MunitCatsEffectVersion % Test,
      "org.mockito" %% "mockito-scala" % MockitoScalaVersion % Test,
      "io.circe" %% "circe-optics" % CirceOpticsVersion % Test
    ),
    // dependency of many libraries, but not used in the project
    undeclaredCompileDependenciesFilter -= moduleFilter("com.chuusai", "shapeless"),
    undeclaredCompileDependenciesFilter -= moduleFilter("org.slf4j", "slf4j-api"),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % TypelevelKindProjectorVersion cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion),
    Compile / run / fork := true,
    Compile / run / mainClass := Some("com.marmaladesky.realworld.Main"),
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    slickPgGen := {
      val args: Seq[String] = spaceDelimited("<arg>").parsed
      val host = args(0)
      val port = args(1)
      val dbName = args(2)
      val schema = args(3)
      val user = args(4)
      val password = args(5)

      val dir = (Compile / scalaSource).value
      val pkg = "com.marmaladesky.realworld.db.gen"
      val profile = "com.marmaladesky.realworld.db.DbProfile"

      val outputDir = pkg.split(".").foldLeft(dir)((f, d) => f / d)

      val cp = (Compile / fullClasspath).value
      val s = streams.value

      runner.value
        .run(
          "com.marmaladesky.realworld.db.gen.AdvancedPgGenerator",
          cp.files,
          Array(host, port, dbName, schema, user, password, "./src/main/scala", pkg, profile, "Tables"),
          s.log
        )
        .failed foreach (sys error _.getMessage)

      val file = outputDir / pkg / "Tables.scala"

      Seq(file)
    }
  )

lazy val slickPgGen = inputKey[Seq[File]]("Generate Tables.scala")

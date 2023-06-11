import mill.scalalib._

object Dependencies {

  object plugins {
    val betterMonadicFor = ivy"com.olegpy::better-monadic-for:0.3.1"
    val kindProjector = ivy"org.typelevel:::kind-projector:0.13.2"
  }

  object test {
    private val munitVersion = "0.7.29"
    private val munitVersionCatsEffect = "1.0.7"

    val munit = ivy"org.scalameta::munit:$munitVersion"
    val munitCatsEffect = ivy"org.typelevel::munit-cats-effect-3:$munitVersionCatsEffect"

    private val testContainersScalaVersion = "0.40.14"
    val testContainersScalaMunit = ivy"com.dimafeng::testcontainers-scala-munit:$testContainersScalaVersion"
  }

  object cats {
    private val catsCoreVersion = "2.9.0"
    private val catsEffectVersion = "3.4.10"
    private val log4CatsVersion = "2.5.0"

    val core = ivy"org.typelevel::cats-core:$catsCoreVersion"
    val effect = ivy"org.typelevel::cats-effect:$catsEffectVersion"
    val log4Cats = ivy"org.typelevel::log4cats-slf4j:$log4CatsVersion"
    val log4CatsNoop = ivy"org.typelevel::log4cats-noop:$log4CatsVersion"
  }

  object cb372 {
    private val catsRetryVersion = "3.1.0"

    val retry = ivy"com.github.cb372::cats-retry:$catsRetryVersion"
  }

  object ciris {
    private val cirisVersion = "3.0.0"

    val core = ivy"is.cir::ciris:$cirisVersion"
  }

  object http4s {
    private val http4sVersion = "0.23.19"

    val dsl = ivy"org.http4s::http4s-dsl:$http4sVersion"
    val circe = ivy"org.http4s::http4s-circe:$http4sVersion"
    val core = ivy"org.http4s::http4s-core:$http4sVersion"
    val server = ivy"org.http4s::http4s-server:$http4sVersion"
  }

  object logback {
    private val logbackVersion = "1.4.5"

    val classic = ivy"ch.qos.logback:logback-classic:$logbackVersion"
  }

  object circe {
    private val circeVersion = "0.14.3"

    val core = ivy"io.circe::circe-core:$circeVersion"
    val generic = ivy"io.circe::circe-generic:$circeVersion"
    val parser = ivy"io.circe::circe-parser:$circeVersion"
  }

  object natchez {
    private val natchezVersion = "0.3.1"
    private val natchezHttp4sVersion = "0.5.0"
    val http4s = ivy"org.tpolecat::natchez-http4s:$natchezHttp4sVersion"
    val openTelemetry = ivy"org.tpolecat::natchez-opentelemetry:$natchezVersion"
  }

  object redis {
    private val redisVersion = "1.4.1"

    val effects = ivy"dev.profunktor::redis4cats-effects:$redisVersion"
    val log4cats = ivy"dev.profunktor::redis4cats-log4cats:$redisVersion"
  }
}

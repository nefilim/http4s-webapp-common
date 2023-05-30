import $file._mill.dependencies
import $ivy.`com.lihaoyi::mill-contrib-buildinfo:`
import $ivy.`com.goyeau::mill-scalafix::0.2.9`
import com.goyeau.mill.scalafix.ScalafixModule
import mill._
import mill.define.{Task, Input, Command, Sources}
import mill.scalalib._
import mill.scalalib.scalafmt._
import $ivy.`com.lihaoyi::mill-contrib-docker:$MILL_VERSION`

import dependencies.Dependencies

private object versions {
  val scala = "2.13.10"
}

object http4sCommon extends WebCommonBaseModule {
  override def ivyDeps = super.ivyDeps() ++ Agg(
    Dependencies.circe.core,
    Dependencies.circe.generic,
    Dependencies.circe.parser,
    Dependencies.http4s.circe,
    Dependencies.http4s.core,
    Dependencies.http4s.dsl,
    Dependencies.redis.effects,
    Dependencies.redis.log4cats,
  )

  object test extends WebCommonTestModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      Dependencies.logback.classic,
    )
  }

  object integration extends WebCommonIntegrationTestModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(
      Dependencies.test.munit,
      Dependencies.logback.classic,
    )
  }
}

trait WebCommonBaseModule extends ScalaModule with ScalafmtModule with ScalafixModule { base =>
  override def scalaVersion: T[String] = versions.scala

  override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
    Dependencies.ciris.core,
    Dependencies.cats.core,
    Dependencies.cats.effect,
  )
  override def scalafixIvyDeps: T[Agg[Dep]] = super.scalafixIvyDeps()

  override def scalacOptions: T[Seq[String]] = super.scalacOptions() ++ Seq(
    "-encoding", "utf8",                 // Specify character encoding used by source files.
    "-Xsource:3",                        // Treat compiler input as Scala source for the specified version.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:deprecation",                // Emit warning and location for usages of deprecated APIs.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Wunused:nowarn",                   // Ensure that a `@nowarn` annotation actually suppresses a warning.
    "-Wdead-code",                       // Warn when dead code is identified.
    "-Wextra-implicit",                  // Warn when more than one implicit parameter section is defined.
    "-Wnumeric-widen",                   // Warn when numerics are widened.
    "-Xlint:implicit-recursion",         // Warn when an implicit resolves to an enclosing self-definition
    "-Wunused:implicits",                // Warn if an implicit parameter is unused.
    "-Wunused:explicits",                // Warn if an explicit parameter is unused.
    "-Wunused:imports",                  // Warn if an import selector is not referenced.
    "-Wunused:locals",                   // Warn if a local definition is unused.
    "-Wunused:params",                   // Warn if a value parameter is unused.
    "-Wunused:patvars",                  // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates",                 // Warn if a private member is unused.
    "-Wvalue-discard",                   // Warn when non-Unit expression results are unused.
    "-Vimplicits",                       // Enables the tek/splain features to make the compiler print implicit resolution chains when no implicit value can be found
    // "-Vtype-diffs",                   // Enables the tek/splain features to turn type error messages (found: X, required: Y) into colored diffs between the two types
    "-Ybackend-parallelism", "8",        // Enable parallelization
    "-Ycache-plugin-class-loader:last-modified",  // Enables caching of classloaders for compiler plugins
    "-Ycache-macro-class-loader:last-modified",    // and macro definitions. This can lead to performance improvements.
    "-Ymacro-annotations" // needed for circe annotations like @JsonCodec
  ) ++ {
//    if (isCI() || withFatalWarns())
    if (withFatalWarns())
      Seq("-Werror")                     // Fail the compilation if there are any warnings.
    else
      Seq.empty[String]
  }

  override def scalacPluginIvyDeps: T[Agg[Dep]] = super.scalacPluginIvyDeps() ++ Agg(
    Dependencies.plugins.betterMonadicFor,
    Dependencies.plugins.kindProjector,
    // DESNOTE(peter, 9/23/22) mill or something seems to add this plugin by default but an older version
    // no longer available for the current scalac version, hence adding explicitly to supersede
    Dependencies.plugins.semanticDBScalaC,
  )

  trait WebCommonTestModule extends Tests with TestModule.Munit with ScalafmtModule with ScalafixModule {
    override def resources: Sources = T.sources {
      super.resources() :+ PathRef(T.workspace / "shared" / "logging")
    }

    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      Dependencies.test.munit,
      Dependencies.test.munitCatsEffect,
      Dependencies.logback.classic,
      Dependencies.cats.log4CatsNoop
    )
    override def scalafixIvyDeps: T[Agg[Dep]] = super.scalafixIvyDeps() ++ Agg(Dependencies.mill.scalafix.organizeImports)
  }

  trait WebCommonIntegrationTestModule extends WebCommonTestModule {
    override def ivyDeps: T[Agg[Dep]] = super.ivyDeps() ++ Agg(
      Dependencies.test.testContainersScalaMunit,
    )
  }
}

private def gitSha(short: Boolean): Command[String] = T.command {
  val shortOpt = Option.when(short)("--short")
  val args = List("git", "rev-parse") ++ shortOpt ++ List("HEAD")
  os.proc(args).call().out.text().trim
}

private def isCI: Input[Boolean] = T.input {
  T.ctx.env.get("CI").exists(ci => "true".equals(ci.toLowerCase))
}

private def withFatalWarns: Input[Boolean] = T.input {
  T.ctx.env.get("FATAL_WARNINGS").exists(ci => "true".equals(ci.toLowerCase))
}

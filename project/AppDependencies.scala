import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "9.19.0"
  private val playVersion          = "30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-frontend-play-$playVersion"            % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"play-frontend-hmrc-play-$playVersion"            % "12.29.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-play-$playVersion" % "3.4.0",
    "org.typelevel" %% "cats-core"                                        % "2.13.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-play-$playVersion" % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                             % "1.21.1",
    "org.scalatestplus"    %% "mockito-4-11"                      % "3.2.17.0",
    "org.scalatestplus"    %% "scalacheck-1-18"                   % "3.2.18.0",
    "org.scalacheck"       %% "scalacheck"                        % "1.18.1",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"             % "1.1.0",
    "io.github.martinhh"   %% "scalacheck-derived"                % "0.10.0"

  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
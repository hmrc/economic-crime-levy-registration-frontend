import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "9.13.0"
  private val playVersion          = "30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-frontend-play-$playVersion"            % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"play-frontend-hmrc-play-$playVersion"            % "12.6.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-play-$playVersion" % "3.3.0",
    "org.typelevel" %% "cats-core"                                        % "2.13.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-play-$playVersion" % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                             % "1.21.1",
    "org.mockito"          %% "mockito-scala"                     % "2.0.0",
    "org.scalatestplus"    %% "scalacheck-1-17"                   % "3.2.18.0",
    "com.danielasfregola"  %% "random-data-generator"             % "2.9",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"             % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}
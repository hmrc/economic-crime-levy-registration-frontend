import sbt._

object AppDependencies {

  private val hmrcBootstrapVersion = "8.5.0"
  private val playVersion          = "30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"   %% s"bootstrap-frontend-play-$playVersion"            % hmrcBootstrapVersion,
    "uk.gov.hmrc"   %% s"play-frontend-hmrc-play-$playVersion"            % "9.1.0",
    "uk.gov.hmrc"   %% s"play-conditional-form-mapping-play-$playVersion" % "2.0.0",
    "org.typelevel" %% "cats-core"                                        % "2.10.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-play-$playVersion" % hmrcBootstrapVersion,
    "org.jsoup"             % "jsoup"                             % "1.17.2",
    "org.mockito"          %% "mockito-scala"                     % "1.17.30",
    "org.scalatestplus"    %% "scalacheck-1-17"                   % "3.2.18.0",
    "com.danielasfregola"  %% "random-data-generator"             % "2.9",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"             % "1.1.0"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}

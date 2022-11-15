import sbt._

object AppDependencies {

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.11.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc"         % "3.27.0-play-28",
    "uk.gov.hmrc" %% "tax-year"                   % "3.0.0"
  )

  val test: Seq[ModuleID]    = Seq(
    "uk.gov.hmrc"         %% "bootstrap-test-play-28"   % "7.11.0",
    "org.jsoup"            % "jsoup"                    % "1.14.3",
    "org.mockito"         %% "mockito-scala"            % "1.17.12",
    "org.scalatestplus"   %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "com.danielasfregola" %% "random-data-generator"    % "2.9"
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test

}

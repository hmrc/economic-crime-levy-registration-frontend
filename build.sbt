import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "economic-crime-levy-registration-frontend"

val silencerVersion = "1.7.7"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(majorVersion := 0)
  .settings(inThisBuild(buildSettings))
  .settings(scoverageSettings: _*)
  .settings(scalaCompilerOptions: _*)
  .settings(
    scalaVersion := "2.13.16",
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.economiccrimelevyregistration.models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "uk.gov.hmrc.economiccrimelevyregistration.models.Binders._"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils._",
      "uk.gov.hmrc.economiccrimelevyregistration.models._",
      "uk.gov.hmrc.economiccrimelevyregistration.controllers.routes._",
      "uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts.routes._",
      "uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 14000,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    (update / evictionWarningOptions).withRank(KeyRanks.Invisible) :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    // concatenate js
  )
  .settings(commands ++= SbtCommands.commands)

lazy val buildSettings = Def.settings(
  scalafmtOnCompile := true,
  useSuperShell := false
)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "test",
    baseDirectory.value / "test-common"
  ),
  fork := true
)

lazy val itSettings: Seq[Def.Setting[_]] = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it",
    baseDirectory.value / "test-common"
  ),
  parallelExecution := false,
  fork := true
)

val excludedScoveragePackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*handlers.*",
  ".*components.*",
  "uk.gov.hmrc.BuildInfo",
  "app.*",
  "prod.*",
  ".*Routes.*",
  ".*testonly.*",
  "testOnlyDoNotUseInAppConf.*",
  ".*viewmodels.govuk.*"
)

val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedFiles := excludedScoveragePackages.mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)

val scalaCompilerOptions: Def.Setting[Task[Seq[String]]] = scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Wconf:cat=feature:ws,cat=optimizer:ws,src=target/.*:s",
  "-Xlint:-byname-implicit"
)

addCommandAlias("runAllChecks", ";clean;compile;scalafmtCheckAll;coverage;test;it:test;scalastyle;coverageReport")

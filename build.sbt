
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName: String = "industry-classification-lookup-frontend"
val silencerVersion = "1.7.16"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models.*;models/.data/..*;view.*;featureswitch.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimumStmtTotal := 80,
  ScoverageKeys.coverageFailOnMinimum := false,
  ScoverageKeys.coverageHighlighting := true
)

lazy val microservice = (project in file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin): _*)
  .settings(defaultSettings(): _*)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(
    PlayKeys.playDefaultPort := 9874,
    Test / fork := true,
    Test / testForkedParallel := false,
    Test / parallelExecution := true,
    Test / logBuffered := false,
    libraryDependencies ++= AppDependencies.apply()
  )
  .settings(
    TwirlKeys.templateImports ++= Seq(
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.govukfrontend.views.html.components.implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components.implicits._",
      "views.ViewUtils._"
    )
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.test
  )
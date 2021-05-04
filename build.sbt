
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName: String = "industry-classification-lookup-frontend"

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;models.*;models/.data/..*;view.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum           := 80,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala,SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .settings(scalaSettings: _*)
  .settings(majorVersion := 0)
  .settings(PlayKeys.playDefaultPort := 9874)
  .settings(publishingSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    fork                       in IntegrationTest := false,
    testForkedParallel         in IntegrationTest := false,
    parallelExecution          in IntegrationTest := false,
    logBuffered                in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    fork                       in Test            := true,
    testForkedParallel         in Test            := false,
    parallelExecution          in Test            := true,
    logBuffered                in Test            := false,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
  .settings(
    scalaVersion                                  := "2.12.12",
    libraryDependencies                           ++= AppDependencies(),
    retrieveManaged                               := true,
    cancelable                 in Global          := true,
    resolvers                                     += Resolver.jcenterRepo,
    addTestReportOption(IntegrationTest, "int-test-reports")
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

javaOptions in IntegrationTest += "-Dlogger.resource=logback-test.xml"

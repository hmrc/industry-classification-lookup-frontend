import sbt._

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "3.4.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.65.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "9.2.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.2.0-play-26",
    "uk.gov.hmrc" %% "play-allowlist-filter" % "1.0.0-play-26",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "8.0.0-play-26",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.6.0-play-26"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2" % scope,
    "org.jsoup" % "jsoup" % "1.13.1" % scope,
    "org.mockito" % "mockito-core" % "3.9.0" % scope,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-26" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope
  )

  def apply() = compile ++ test()
}

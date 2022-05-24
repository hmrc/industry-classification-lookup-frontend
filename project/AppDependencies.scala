
import sbt._

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.20.0",
    "uk.gov.hmrc" %% "play-ui" % "9.8.0-play-28",
    "uk.gov.hmrc" %% "play-frontend-hmrc" % "3.14.0-play-28",
    "uk.gov.hmrc" %% "play-allowlist-filter" % "1.0.0-play-28",
    "uk.gov.hmrc" %% "simple-reactivemongo" % "8.0.0-play-28",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.11.0-play-28"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
    "com.github.tomakehurst" % "wiremock-jre8-standalone" % "2.31.0" % scope,
    "org.jsoup" % "jsoup" % "1.14.3" % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % scope,
    "org.mockito" % "mockito-core" % "4.1.0" % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-28" % scope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % scope
  )

  def apply() = compile ++ test()
}

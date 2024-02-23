
import sbt._

private object AppDependencies {
  val compile = Seq(
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "7.12.0",
    "uk.gov.hmrc" %% "play-frontend-hmrc-play-28" % "8.5.0",
    "uk.gov.hmrc" %% "play-allowlist-filter" % "1.1.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.74.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.12.0-play-28"
  )

  def test(scope: String = "test,it") = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % "2.27.2" % scope,
    "org.jsoup" % "jsoup" % "1.13.1" % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % scope,
    "org.mockito" % "mockito-core" % "3.3.0" % scope,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % "0.74.0" % scope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.36.8" % scope
  )

  def apply() = compile ++ test()
}

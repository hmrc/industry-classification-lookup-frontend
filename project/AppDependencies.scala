
import sbt._

private object AppDependencies {
  private val playVersion = "play-30"
  val mongoDbVersion   = "1.9.0"
  val bootstrapVersion = "9.9.0"
  val frontendPlayVersion = "11.11.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-frontend-$playVersion"       % bootstrapVersion,
    "uk.gov.hmrc"             %% s"play-frontend-hmrc-$playVersion"       % frontendPlayVersion,
    "uk.gov.hmrc"             %% "play-allowlist-filter"                  % "1.3.0",
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-$playVersion"               % mongoDbVersion,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "org.apache.commons"      % "commons-text"                            % "1.11.0"
  )

  def test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-test-$playVersion" % bootstrapVersion  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "7.0.1"           % Test,
    "org.jsoup"               % "jsoup"                         % "1.18.2"          % Test,
    "org.scalatestplus"       %% "mockito-3-4"                  % "3.2.10.0"        % Test,
    "org.mockito"             % "mockito-core"                  % "5.14.2"          % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.64.8"          % Test
  )

  def apply(): Seq[sbt.ModuleID] = compile ++ test
}

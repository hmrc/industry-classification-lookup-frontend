
import sbt.*

private object AppDependencies {
  private val playVersion = "play-30"
  val mongoDbVersion   = "2.14.0"
  val bootstrapVersion = "10.9.0"
  val frontendPlayVersion = "13.16.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-frontend-$playVersion"       % bootstrapVersion,
    "uk.gov.hmrc"             %% s"play-frontend-hmrc-$playVersion"       % frontendPlayVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-$playVersion"               % mongoDbVersion,
    "uk.gov.hmrc"             %% "play-conditional-form-mapping-play-30"  % "3.5.0",
    "org.apache.commons"      % "commons-text"                            % "1.15.0"
  )

  def test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-test-$playVersion" % bootstrapVersion  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"           % "7.0.2"           % Test,
    "org.jsoup"               % "jsoup"                         % "1.24.1"          % Test,
    "org.scalatestplus"       %% "mockito-3-4"                  % "3.2.10.0"        % Test,
    "org.mockito"             % "mockito-core"                  % "5.24.0"          % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                  % "0.64.8"          % Test
  )

  def apply(): Seq[sbt.ModuleID] = compile ++ test
}

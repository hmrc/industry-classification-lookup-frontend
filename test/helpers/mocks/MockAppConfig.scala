/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers.mocks

import config.AppConfig
import helpers.UnitTestSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}

trait MockAppConfig extends GuiceOneAppPerSuite {
  self: UnitTestSpec =>

  val env: Environment          = Environment.simple()
  val fakeConfig: Configuration = Configuration.load(env)

  implicit val mockConfig: AppConfig = app.injector.instanceOf[AppConfig]

}

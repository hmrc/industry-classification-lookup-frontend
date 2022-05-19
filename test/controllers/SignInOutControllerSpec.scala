/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class SignInOutControllerSpec extends UnitTestSpec with MockMessages with MockAppConfig {

  val cRUrl = "http://localhost:12345/"
  val cRUri = "test-uri"

  class Setup {
    val controller: SignInOutController = new SignInOutController(
      mcc = mockMessasgesControllerComponents,
      authConnector = mockAuthConnector,
      journeyService = mockJourneyService,
      sicSearchService = mockSicSearchService
    )(
      ec = global,
      appConfig = mockConfig
    ) {
      override lazy val loginURL = "/test/login"
    }
  }

  "signOut" should {
    "return a 303 and redirect to questionnaire" in new Setup {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val signOutUrl = "http://localhost:9514/feedback/vat-registration"

      AuthHelpers.showWithAuthorisedUser(controller.signOut, request) {
        result =>
          status(result) mustBe 303
          redirectLocation(result) mustBe Some(signOutUrl)
      }
    }
  }
}

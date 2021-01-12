/*
 * Copyright 2021 HM Revenue & Customs
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
import helpers.mocks.MockMessages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class SignInOutControllerSpec extends UnitTestSpec with MockMessages {

  val cRUrl = "http://localhost:12345/"
  val cRUri = "test-uri"

  class Setup {
    val controller: SignInOutController = new SignInOutController(
      mcc = mockMessasgesControllerComponents,
      authConnector = mockAuthConnector,
      journeyService = mockJourneyService,
      sicSearchService = mockSicSearchService,
      servicesConfig = mockServicesConfig
    ) {
      override lazy val loginURL = "/test/login"

      override lazy val compRegFEURL: String = cRUrl
      override lazy val compRegFEURI: String = cRUri
    }
  }

  "postSignIn" should {

    "return a 303 and redirect to post sign in" in new Setup {

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val url = s"$cRUrl$cRUri/post-sign-in"

      AuthHelpers.showWithAuthorisedUser(controller.postSignIn, request) {
        result =>
          status(result) mustBe 303
          redirectLocation(result) mustBe Some(url)
      }
    }
  }

  "signOut" should {

    "return a 303 and redirect to questionnaire" in new Setup {

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
      val url = s"$cRUrl$cRUri/questionnaire"

      AuthHelpers.showWithAuthorisedUser(controller.signOut, request) {
        result =>
          status(result) mustBe 303
          redirectLocation(result) mustBe Some(url)
      }
    }
  }
}

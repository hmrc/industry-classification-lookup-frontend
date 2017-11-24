/*
 * Copyright 2017 HM Revenue & Customs
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
import builders.AuthBuilders
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.WithFakeApplication

class SignInOutControllerSpec extends ControllerSpec with WithFakeApplication with AuthBuilders {

  val cRUrl = "http://localhost:12345/"
  val cRUri = "test-uri"

  class Setup {
    val controller: SignInOutController = new SignInOutController {
      override val compRegFEURL: String = cRUrl
      override val compRegFEURI: String = cRUri
      override val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
      override val authConnector: AuthConnector = mockAuthConnector
    }
  }

  "postSignIn" should {

    "return a 303 and redirect to post sign in" in new Setup {

      val request = FakeRequest()
      val url = s"$cRUrl$cRUri/post-sign-in"

      requestWithAuthorisedUser(controller.postSignIn, mockAuthConnector, request){
        result =>
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some(url)
      }
    }
  }

  "signOut" should {

    "return a 303 and redirect to questionnaire" in new Setup {

      val request = FakeRequest()
      val url = s"$cRUrl$cRUri/questionnaire"

      requestWithAuthorisedUser(controller.signOut, mockAuthConnector, request){
        result =>
          status(result) shouldBe 303
          redirectLocation(result) shouldBe Some(url)
      }
    }
  }
}
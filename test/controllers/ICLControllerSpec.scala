/*
 * Copyright 2018 HM Revenue & Customs
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

import config.AppConfig
import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class ICLControllerSpec extends UnitTestSpec with MockAppConfig with MockMessages {

  trait Setup {
    val controller: ICLController = new ICLController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig        = mockAppConfig
      override val journeyService: JourneyService       = mockJourneyService
      override val authConnector: AuthConnector         = mockAuthConnector
      override val messagesApi: MessagesApi             = MockMessages
      override val sicSearchService: SicSearchService   = mockSicSearchService
    }
  }

  val sessionId = "session-12345"
  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  "withSessionId" should {
    "supply the sessionId to the function parameter and return the supplied result" in new Setup {
      val suppliedFunction: String => Future[Result] = sessionId => Future.successful(Ok(sessionId))

      assertFutureResult(controller.withSessionId(suppliedFunction)(requestWithSessionId)) { res =>
        status(res)          mustBe OK
        contentAsString(res) mustBe sessionId
      }
    }

    "return a Bad Request when the request does not contain a session id" in new Setup {
      val suppliedFunction: String => Future[Result] = _ => Future.successful(Ok)

      assertFutureResult(controller.withSessionId(suppliedFunction)(FakeRequest())) { res =>
        status(res) mustBe BAD_REQUEST
        contentAsString(res) mustBe "SessionId is missing from request"
      }
    }
  }
}

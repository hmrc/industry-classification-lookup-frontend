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

package controllers

import config.AppConfig
import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import play.api.mvc.{Result, Results}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ICLControllerSpec extends UnitTestSpec with MockAppConfig with MockMessages {

  trait Setup {

    object TestICLController extends ICLController(mockMessasgesControllerComponents) {
      implicit val appConfig: AppConfig      = app.injector.instanceOf[AppConfig]
      val authConnector: AuthConnector       = mockAuthConnector
      val journeyService: JourneyService     = mockJourneyService
      val sicSearchService: SicSearchService = mockSicSearchService
      val servicesConfig: ServicesConfig     = mockServicesConfig

      override lazy val loginURL = "/test/login"
    }

  }

  val sessionId = "session-12345"

  "withSessionId" should {
    "supply the sessionId to the function parameter and return the supplied result" in new Setup {
      val suppliedFunction: String => Future[Result] = sessionId => Future.successful(Results.Ok(sessionId))

      assertFutureResult(
        TestICLController.withSessionId(suppliedFunction)(hc.copy(sessionId = Some(SessionId(sessionId))))
      ) { res =>
        status(res) mustBe OK
        contentAsString(res) mustBe sessionId
      }
    }

    "return a Bad Request when the request does not contain a session id" in new Setup {
      val suppliedFunction: String => Future[Result] = _ => Future.successful(Results.Ok)

      assertFutureResult(TestICLController.withSessionId(suppliedFunction)(hc)) { res =>
        status(res) mustBe BAD_REQUEST
        contentAsString(res) mustBe "SessionId is missing from request"
      }
    }
  }
}

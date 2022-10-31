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

package controllers.test

import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import views.html.test.SetupJourneyView

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestSetupControllerSpec extends UnitTestSpec with GuiceOneAppPerSuite with MockAppConfig with MockMessages {

  class Setup {

    lazy val testView = app.injector.instanceOf[SetupJourneyView]

    val controller: TestSetupController = new TestSetupController(
      mcc = mockMessasgesControllerComponents,
      authConnector = mockAuthConnector,
      journeyService = mockJourneyService,
      sicSearchService = mockSicSearchService,
      view = testView
    )(
      ec = global,
      appConfig = mockConfig
    ) {
      override lazy val loginURL = "/test/login"
    }

    val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)
  }

  val lang = "en"
  val journeyId = "testJourneyId"
  val sessionId = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)
  val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), LocalDateTime.now())

  val journeyName: String = JourneyData.QUERY_BUILDER
  val dataSet: String = JourneyData.ONS
  val journeySetup = JourneySetup("foo", queryParser = None, queryBooster = None, 1)

  val sicStore = SicStore(
    sessionId,
    Some(SearchResults("test-query", 1, List(SicCode("19283", "Search Sic Code Result Description", "Search Sic Code Result Description")), List()))
  )

  "show" should {

    "return a 200 and render the SetupJourneyView page when a journey has already been initialised" in new Setup {

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithSessionId) { result =>
        status(result) mustBe 200
      }
    }

    "return a 200 and render the SetupJourneyView page when a journey has not been initialised" in new Setup {

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithSessionId) { result =>
        status(result) mustBe 200
      }
    }
  }

  "submit" should {

    "return a 400 when the form is empty" in new Setup {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "journey" -> ""
      )

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), request) { result =>
        status(result) mustBe 400
      }
    }

    s"return a 303 and redirect to ${controllers.routes.ChooseActivityController.show(journeyId)} when a journey is initialised" in new Setup {

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody(
        "queryParser" -> "false",
        "dataSet" -> dataSet,
        "amountOfResults" -> "5"
      )

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)
      when(mockJourneyService.updateJourneyWithJourneySetup(any(), any())).thenReturn(Future.successful(journeySetup))

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ChooseActivityController.show(journeyId).toString)
      }
    }
  }

  "testSetup" must {
    "redirect to the test setup show page" in new Setup {
      when(mockJourneyService.initialiseJourney(any(), eqTo(lang))(any())) thenReturn Future.successful(Json.parse("""{}"""))

      AuthHelpers.showWithAuthorisedUser(controller.testSetup, requestWithSessionId) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("/session-12345/setup-journey")
      }
    }

    "return a Bad Request when there is no session" in new Setup {
      AuthHelpers.showWithAuthorisedUser(controller.testSetup, FakeRequest()) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "endOfJourney" must {
    "return Ok when a journey exists and have a session" in new Setup {

      val sicCodeChoices = Some(List(SicCodeChoice(SicCode("12345", "test description", "test description"))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(sicCodeChoices))

      AuthHelpers.showWithAuthorisedUser(controller.endOfJourney(journeyId), requestWithSessionId) { result =>
        status(result) mustBe OK
        contentAsString(result) mustBe "End of Journey" + Json.prettyPrint(Json.toJson(sicCodeChoices))
      }
    }

    "return a Bad Request when there is no session" in new Setup {
      AuthHelpers.showWithAuthorisedUser(controller.endOfJourney(journeyId), FakeRequest()) { result =>
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "SessionId is missing from request"
      }
    }

    "return a Exception when there is no journey setup" in new Setup {
      intercept[Exception](AuthHelpers.showWithAuthorisedUser(controller.endOfJourney(journeyId), requestWithSessionId) { result =>
        status(result) mustBe "this will never run so test will pass"
      })
    }
  }
}

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

package controllers.internal

import helpers.UnitTestSpec
import helpers.auth.AuthHelpers
import helpers.mocks.{MockAppConfig, MockMessages}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import models.{SicCode, SicCodeChoice}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiControllerSpec extends UnitTestSpec with MockMessages with MockAppConfig with AuthHelpers {

  override val authConnector: AuthConnector = mockAuthConnector

  trait Setup {
    val controller: ApiController = new ApiController(
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

  "journeyInitialisation" should {
    val lang = "en"
    val validRequestedJson: JsValue = Json.parse(
      """{
        | "redirectUrl": "test/url",
        | "journeySetupDetails": {
        |   "amountOfResults": 50,
        |   "customMessages": {
        |     "summary": {
        |       "heading": "value1",
        |       "lead": "value2"
        |     }
        |   }
        | }
        |}""".stripMargin)
    val validRequestedJsonWithoutCustomMessages = Json.parse(
      """{
        | "redirectUrl":"test/url",
        |  "journeySetupDetails": {
        |    "amountOfResults": 50
        |  }
        |}""".stripMargin
    )

    val requestWithSessionId: FakeRequest[JsValue] = FakeRequest().withSessionId("test-session-id").withBody(validRequestedJson)

    "return 200 with json" when {

      val expectedJsonResponse = Json.obj(
        "journeyStartUri" -> "/test/uri/",
        "fetchResultsUri" -> "/test/uri/"
      )

      "journey is initialised with custom messages" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        when(mockJourneyService.initialiseJourney(any(), eqTo(lang))(any(), any())) thenReturn Future.successful(expectedJsonResponse)

        val result: Future[Result] = controller.journeyInitialisation()(requestWithSessionId)

        status(result) mustBe OK
        contentAsJson(result) mustBe expectedJsonResponse
      }

      "journey is initialised without custom messages" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val fakeRequest: FakeRequest[JsValue] = FakeRequest()
          .withSessionId("test-session-id")
          .withBody(validRequestedJsonWithoutCustomMessages)

        when(mockJourneyService.initialiseJourney(any(), eqTo(lang))(any(), any())) thenReturn Future.successful(expectedJsonResponse)

        val result: Future[Result] = controller.journeyInitialisation()(fakeRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe expectedJsonResponse
      }
    }

    "return 400" when {
      "payload provided is not valid" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val invalidRequest: FakeRequest[JsValue] = FakeRequest()
          .withSessionId("test-session-id")
          .withBody(
            Json.parse(
              """{
                | "customMessages": {
                |   "summary": {
                |     "heading": "value1",
                |     "lead": "value2"
                |   }
                | }
                |}""".
                stripMargin)
          )

        val result: Future[Result] = controller.journeyInitialisation()(invalidRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("redirectUrl")
      }

      "sessionId is missing" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val requestWithoutSessionId: FakeRequest[JsValue] = FakeRequest().withBody(validRequestedJson)

        val result: Future[Result] = controller.journeyInitialisation()(requestWithoutSessionId)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "SessionId is missing from request"
      }
    }
  }

  "fetchResults" must {

    val journeyId = "testJourneyId"
    val sessionId = "testSessionId"
    val journeyData = JourneyData(Identifiers(journeyId, sessionId), "redirectUrl", JourneySetup(), LocalDateTime.now())

    "return 200 with json" when {
      "the journey exists and there have been sic codes selected" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val sicCode = SicCode("12345", "test description", "test description")
        val sicCodeChoice = SicCodeChoice(sicCode, List("123", "456", "789"), List("123", "456", "789"))
        val sicCodeChoices = List(sicCodeChoice)

        val sicCodeChoicesJson: JsValue = Json.parse(
          """
            |{
            |  "sicCodes":[
            |     {
            |       "code":"12345",
            |       "desc":"test description",
            |       "descCy":"test description",
            |       "indexes":[
            |         "123",
            |         "456",
            |         "789"
            |       ],
            |       "indexesCy":[
            |         "123",
            |         "456",
            |         "789"
            |       ]
            |     }
            |  ]
            |}
          """.stripMargin)

        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
          .withMethod("POST")
          .withSessionId(sessionId)

        when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)
        when(mockSicSearchService.retrieveChoices(any())(any())) thenReturn Future.successful(Some(sicCodeChoices))

        val result: Future[Result] = controller.fetchResults(journeyId)(fakeRequest)
        status(result) mustBe OK
        contentAsJson(result) mustBe sicCodeChoicesJson
      }
    }

    "return 400" when {
      "there is no session id in the request made" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val result: Future[Result] = controller.fetchResults(journeyId)(FakeRequest().withMethod("POST"))
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe "SessionId is missing from request"
      }
    }

    "return exception" when {
      "there is not a journey setup for the requested journeyId" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withMethod("POST").withSessionId(sessionId)

        when(mockJourneyService.getJourney(any())(any())) thenReturn Future.failed(new RuntimeException)

        intercept[Exception](await(controller.fetchResults(journeyId)(fakeRequest)))
      }
    }
    "return 404" when {
      "there is no sic code choices in the sic store" in new Setup {
        mockAuthorisedUser(Future.successful({}))
        val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
          .withMethod("POST")
          .withSessionId(sessionId)

        when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)
        when(mockSicSearchService.retrieveChoices(any())(any())) thenReturn Future.successful(None)

        val result: Future[Result] = controller.fetchResults(journeyId)(fakeRequest)
        status(result) mustBe NOT_FOUND
      }
    }
  }
}

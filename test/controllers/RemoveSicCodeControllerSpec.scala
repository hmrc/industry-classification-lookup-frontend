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

import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import views.html.pages.removeActivityConfirmation

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveSicCodeControllerSpec extends UnitTestSpec with GuiceOneAppPerSuite with MockAppConfig with MockMessages {

  class Setup {

    lazy val testView = app.injector.instanceOf[removeActivityConfirmation]

    val controller: RemoveSicCodeController = new RemoveSicCodeController(
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
  }

  val journeyId   = "testJourneyId"
  val sessionId   = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)
  val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), Instant.now())

  val getRequestWithSessionId: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withMethod("GET").withSessionId(sessionId)
  val postRequestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)

  def formRequestWithSessionId(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] =
    postRequestWithSessionId.withMethod("POST").withFormUrlEncodedBody("removeCode" -> answer)

  val sicCodeCode        = "12345"
  val sicCodeDescription = "some description"
  val sicCode            = SicCode(sicCodeCode, sicCodeDescription, sicCodeDescription)
  val sicCodeChoice      = SicCodeChoice(sicCode, List("fake item"), List("fake item"))
  val searchResults =
    SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Fake Sector", "Cy business sector", 1)))

  "show" should {
    "return a 200 when the page is rendered" in new Setup {

      when(mockSicSearchService.removeChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId, sicCodeCode), getRequestWithSessionId) { result =>
        status(result) mustBe OK
      }
    }

    "redirect to search page when the supplied sic code doesn't exist" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId, "Unknown"), getRequestWithSessionId) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.ChooseActivityController.show(journeyId, Some(true)).url
        )
      }
    }
  }

  "submit" should {
    "return a 400 if no field is selected" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("")) {
        result =>
          status(result) mustBe BAD_REQUEST
          verify(mockSicSearchService, times(0)).removeChoice(any(), any())(any())
      }
    }
    "remove choice and redirect to the confirmation page if yes is selected" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockSicSearchService.removeChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("yes")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.show(journeyId).url)
          verify(mockSicSearchService, times(1)).removeChoice(any(), any())(any())
      }
    }
    "redirect to the confirmation page if no is selected" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("no")) {
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.show(journeyId).url)
          verify(mockSicSearchService, times(0)).removeChoice(any(), any())(any())
      }
    }
  }

}

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

import java.time.LocalDateTime

import config.AppConfig
import helpers.mocks.{MockAppConfig, MockMessages}
import helpers.UnitTestSpec
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class RemoveSicCodeControllerSpec extends UnitTestSpec with MockAppConfig with MockMessages {

  class Setup {
    val controller: RemoveSicCodeController = new RemoveSicCodeController with I18nSupport {
      override val loginURL = "/test/login"

      override implicit val appConfig: AppConfig      = mockAppConfig
      override val sicSearchService: SicSearchService = mockSicSearchService
      override val authConnector: AuthConnector       = mockAuthConnector
      override val messagesApi: MessagesApi           = MockMessages
      override val journeyService: JourneyService     = mockJourneyService
    }
  }

  val journeyId = "testJourneyId"
  val sessionId = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)
  val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), LocalDateTime.now())

  val requestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSessionId(sessionId)
  def formRequestWithSessionId(answer: String): FakeRequest[AnyContentAsFormUrlEncoded] = requestWithSessionId.withFormUrlEncodedBody("removeCode" -> answer)

  val sicCodeCode = "12345"
  val sicCodeDescription = "some description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription)
  val sicCodeChoice = SicCodeChoice(sicCode, List("fake item"))
  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Fake Sector", 1)))

  "show" should {
    "return a 200 when the page is rendered" in new Setup {

      when(mockSicSearchService.removeChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId, sicCodeCode), requestWithSessionId){
        result =>
          status(result) mustBe OK
      }
    }

    "redirect to search page when the supplied sic code doesn't exist" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId, "Unknown"), requestWithSessionId){
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.ChooseActivityController.show(journeyId, Some(true)).url)
      }
    }
  }

  "submit" should {
    "return a 400 if no field is selected" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("")){
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

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("yes")){
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.show(journeyId).url)
          verify(mockSicSearchService, times(1)).removeChoice(any(), any())(any())
      }
    }
    "redirect to the confirmation page if no is selected" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId, sicCodeCode), formRequestWithSessionId("no")){
        result =>
          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.show(journeyId).url)
          verify(mockSicSearchService, times(0)).removeChoice(any(), any())(any())
      }
    }
  }

}

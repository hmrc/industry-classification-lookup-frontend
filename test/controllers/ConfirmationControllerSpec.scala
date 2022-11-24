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

import featureswitch.core.config.{FeatureSwitching, WelshLanguage}
import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models._
import models.setup.messages.{CustomMessages, Summary}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import views.html.pages.confirmation

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmationControllerSpec extends UnitTestSpec with MockAppConfig with MockMessages with FeatureSwitching {

  class Setup {

    lazy val testView = app.injector.instanceOf[confirmation]

    val controller: ConfirmationController = new ConfirmationController(
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

  val journeyId = "testJourneyId"
  val sessionId = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)
  val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), LocalDateTime.now())

  val englishSummary: Summary = Summary(Some("En heading"), Some("En lead"), Some("En hint"))
  val welshSummary: Summary = Summary(Some("Cy heading"), Some("Cy lead"), Some("Cy hint"))
  val journeyDataWithCustomMessages = JourneyData(
    identifiers, "redirectUrl",
    JourneySetup(customMessages = Some(CustomMessages(summary = Some(englishSummary), summaryCy = Some(welshSummary)))),
    LocalDateTime.now()
  )

  val getRequestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withMethod("GET").withSessionId(sessionId)
  val postRequestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withMethod("POST").withSessionId(sessionId)

  val sicCodeCode = "12345"
  val sicCodeDescription = "some description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription, sicCodeDescription)
  val sicCodeChoice = SicCodeChoice(sicCode, List("fake item"), List("fake item"))
  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Fake Sector", "Cy business sector", 1)))

  "show" should {
    "return a 200 when a SicStore is returned from mongo" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), getRequestWithSessionId) {
        result =>
          status(result) mustBe 200
      }
    }
    "return a 200 with EN heading when no language cookie set and both en/cy custom messages available in journey data" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyDataWithCustomMessages)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), getRequestWithSessionId) {
        result =>
          status(result) mustBe 200
          Jsoup.parse(Helpers.contentAsString(result))
            .select("h1").text() mustBe englishSummary.heading.get
      }
    }
    "return a 200 and CY heading welsh language cookie is set and FS is enabled" in new Setup {
      enable(WelshLanguage)
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyDataWithCustomMessages)

      val requestWithWelshLangCookie = getRequestWithSessionId.withCookies(Cookie("PLAY_LANG", "cy"))
      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithWelshLangCookie) {
        result =>
          status(result) mustBe 200
          Jsoup.parse(Helpers.contentAsString(result))
            .select("h1").text() mustBe welshSummary.heading.get
      }
      disable(WelshLanguage)
    }
    "return a 200 and EN heading when welsh lang cookie set but FS is disabled" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyDataWithCustomMessages)

      val requestWithWelshLangCookie = getRequestWithSessionId.withCookies(Cookie("PLAY_LANG", "cy"))
      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), requestWithWelshLangCookie) {
        maybeResult =>
          status(maybeResult) mustBe 200
          Jsoup.parse(Helpers.contentAsString(maybeResult))
            .select("h1").text() mustBe englishSummary.heading.get
      }
    }
    "return a 303 when previous choices are not found in mongo" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      AuthHelpers.showWithAuthorisedUser(controller.show(journeyId), getRequestWithSessionId) {
        result =>
          status(result) mustBe 303
      }
    }
  }

  "submit" should {
    "redirect out of the service to the redirect url setup via the api" in new Setup {
      when(mockSicSearchService.retrieveChoices(eqTo(journeyId))(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      when(mockJourneyService.getRedirectUrl(any())) thenReturn Future.successful("redirect-url")

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), postRequestWithSessionId.withFormUrlEncodedBody()) { result =>
        status(result) mustBe 303
        redirectLocation(result) mustBe Some("redirect-url")
      }
    }

    "return a 400 when more than 4 choices have been made" in new Setup {
      when(mockSicSearchService.retrieveChoices(eqTo(journeyId))(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice, sicCodeChoice))))

      when(mockJourneyService.getJourney(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = postRequestWithSessionId.withFormUrlEncodedBody()

      AuthHelpers.submitWithAuthorisedUser(controller.submit(journeyId), request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "withCurrentUsersChoices" should {
    "return a 303 and redirect to SicSearch when a SicStore does not exist" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Results.Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId).url)
    }
    "return a 303 and redirect to SicSearch when a SicStore does exist but does not contain any choices" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List())))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Results.Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId).url)
    }
    "return a 200 when a SicStore does exist and the choices list is populated" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCodeChoice))))

      val f: List[SicCodeChoice] => Future[Result] = _ => Future.successful(Results.Ok)
      val result: Future[Result] = controller.withCurrentUsersChoices(identifiers)(f)

      status(result) mustBe OK
    }
  }
}

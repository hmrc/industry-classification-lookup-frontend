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
import helpers.auth.AuthHelpers
import helpers.mocks.{MockAppConfig, MockMessages}
import models._
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Cookie, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.chooseActivity

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ChooseActivityControllerSpec extends UnitTestSpec with GuiceOneAppPerSuite with MockAppConfig with MockMessages {

  class Setup extends CodeMocks with AuthHelpers {
    override val authConnector: AuthConnector = mockAuthConnector

    lazy val testView = app.injector.instanceOf[chooseActivity]

    val controller: ChooseActivityController = new ChooseActivityController(
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

    val getRequestWithSession: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSessionId(sessionId).withMethod("GET").withCookies(Cookie("PLAY_LANG", "en"))
    val postRequestWithSession: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest().withSessionId(sessionId).withMethod("POST").withCookies(Cookie("PLAY_LANG", "en"))
  }

  val lang        = "en"
  val journeyId   = "testJourneyId"
  val sessionId   = "session-12345"
  val identifiers = Identifiers(journeyId, sessionId)

  val journeyData = JourneyData(identifiers, "redirectUrl", JourneySetup(), Instant.now())

  val SECTOR_A = "A"
  val query    = "testQuery"
  val sicCode  = SicCode("12345", "Test Description", "Test Description")
  val sicCode2 = SicCode("12345", "Test Description2", "Test Description2")

  val searchResults =
    SearchResults(query, 1, List(sicCode), List(Sector(SECTOR_A, "Fake Sector", "Cy business sector", 1)))
  val noSearchResults = SearchResults(query, 0, List(), List())
  val multipleSearchResults = SearchResults(
    query,
    2,
    List(sicCode, sicCode2),
    List(Sector("A", "Fake Sector", "Cy business sector", 1), Sector("B", "Faker sector", "Cy business sector", 1))
  )

  "show without results" should {
    "return a 303 for an unauthorised user" in new Setup {
      showWithUnauthorisedUser(controller.show(journeyId), FakeRequest()) { response =>
        status(response) mustBe SEE_OTHER
      }
    }

    "return a 200 with a new search for an authorised user" in new Setup {

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.show(journeyId), getRequestWithSession) { response: Future[Result] =>
        status(response) mustBe OK
        val document = Jsoup.parse(contentAsString(response))
        document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
        document.getElementById("sicSearch").attr("value") mustBe ""
        an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
        an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
      }
    }
  }

  "show with results" should {
    "return a 200 with performed search returning nothing for an authorised user" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.show(journeyId, Some(true)), postRequestWithSession) {
        response: Future[Result] =>
          status(response) mustBe OK
          val document = Jsoup.parse(contentAsString(response))
          document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
          document.getElementById("sicSearch").attr("value") mustBe ""
          an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
          an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
      }
    }

    "return a 200 for an authorised user when there is only one sic code found is found and redirect to confirmation page" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.show(journeyId, Some(true)), postRequestWithSession) {
        response: Future[Result] =>
          status(response) mustBe OK
      }
    }

    "return a 200 for an authorised user with multiple sic codes being returned and show the choose activity page and verify sicSearch bar is displayed" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(multipleSearchResults)))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.show(journeyId, Some(true)), postRequestWithSession) {
        response: Future[Result] =>
          status(response) mustBe OK
          val document = Jsoup.parse(contentAsString(response))
          document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
          document.getElementById("sicSearch").attr("value") mustBe multipleSearchResults.query
          an[Exception] mustBe thrownBy(document.getElementById("no-result").text)
          document.getElementById("result-count").text mustBe s"${multipleSearchResults.numFound.toString} results"
      }
    }

    s"return a 200 and show the choose activity page with no search results found and verify sicSearch bar is displayed" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(any())(any()))
        .thenReturn(Future.successful(Some(noSearchResults)))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.show(journeyId, Some(true)), postRequestWithSession) { result =>
        status(result) mustBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("sicSearch").attr("name") mustBe "sicSearch"
        document.getElementById("sicSearch").attr("value") mustBe noSearchResults.query
        document.getElementById("no-result").text mustBe "0 results"
        an[Exception] mustBe thrownBy(document.getElementById("result-count").text)
      }
    }
  }

  "Submit a search" should {
    "return a 303 with the search results page" in new Setup {

      when(mockSicSearchService.search(any(), eqTo(query), any(), eqTo(lang))(any(), any(), any()))
        .thenReturn(Future.successful(multipleSearchResults.numFound))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = postRequestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      requestWithAuthorisedUser(controller.submit(journeyId, Some("test")), request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId, Some(true)).url)
      }
    }

    "return a 303 with the choices list page for an authorised user when the search returns only 1 result" in new Setup {

      when(mockSicSearchService.search(any(), eqTo(query), any(), eqTo(lang))(any(), any(), any()))
        .thenReturn(Future.successful(searchResults.numFound))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = postRequestWithSession.withFormUrlEncodedBody(
        "sicSearch" -> query
      )

      requestWithAuthorisedUser(controller.submit(journeyId, Some("test")), request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmationController.show(journeyId).url)
      }
    }

    "return a 400 for an authorised user without a sic search" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.submit(journeyId, Some("test")), postRequestWithSession) {
        response: Future[Result] =>
          status(response) mustBe BAD_REQUEST
      }
    }
  }

  "Submit an activity" should {
    "return a 303 for an unauthorised user" in new Setup {
      submitWithUnauthorisedUser(controller.submit(journeyId), FakeRequest().withFormUrlEncodedBody()) { request =>
        status(request) mustBe SEE_OTHER
      }
    }

    "return the choices list page for an authorised user with a selected option" in new Setup {
      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.lookupSicCodes(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(1))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = postRequestWithSession.withFormUrlEncodedBody(
        "code[0]" -> "12345-test description-test description"
      )

      requestWithAuthorisedUser(controller.submit(journeyId), request) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ConfirmationController.show(journeyId).url)
      }
    }

    "return a 400 for an authorised user with no option selected" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(ArgumentMatchers.anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = postRequestWithSession.withFormUrlEncodedBody(
        "code" -> ""
      )

      requestWithAuthorisedUser(controller.submit(journeyId), request) { result =>
        status(result) mustBe BAD_REQUEST
      }
    }
  }

  "filter" should {
    s"return a 303 and redirect to ${routes.ChooseActivityController.show(journeyId)} when search results are found" in new Setup {

      when(mockSicSearchService.retrieveSearchResults(eqTo(journeyId))(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.search(any(), eqTo(query), any(), eqTo(lang))(any(), any(), any()))
        .thenReturn(Future.successful(1))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.filter(journeyId, SECTOR_A), postRequestWithSession) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId, Some(true)).url)
      }
    }

    s"return a 303 and redirect to ${routes.ChooseActivityController.show(journeyId)} when no search results are found" in new Setup {
      when(mockSicSearchService.retrieveSearchResults(eqTo(journeyId))(any()))
        .thenReturn(Future.successful(Some(noSearchResults)))

      when(mockSicSearchService.search(any(), eqTo(query), any(), eqTo(lang))(any(), any(), any()))
        .thenReturn(Future.successful(0))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.filter(journeyId, SECTOR_A), postRequestWithSession) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId, Some(true)).url)
      }
    }
  }

  "clearFilter" should {
    "refresh the Business Activity Lookup page" in new Setup {
      when(mockSicSearchService.retrieveSearchResults(eqTo(journeyId))(any()))
        .thenReturn(Future.successful(Some(searchResults)))

      when(mockSicSearchService.search(any(), eqTo(query), any(), eqTo(lang))(any(), any(), any()))
        .thenReturn(Future.successful(1))

      when(mockJourneyService.getJourney(ArgumentMatchers.any())(any())) thenReturn Future.successful(journeyData)

      requestWithAuthorisedUser(controller.clearFilter(journeyId), postRequestWithSession) { result =>
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ChooseActivityController.show(journeyId, Some(true)).url)
      }
    }
  }
}

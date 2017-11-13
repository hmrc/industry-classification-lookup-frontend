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
import models.{SearchResults, SicCode, SicStore}
import play.api.i18n.MessagesApi
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.WithFakeApplication
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ConfirmationControllerSpec extends ControllerSpec with WithFakeApplication with AuthBuilders {

  trait Setup {
    val controller: ConfirmationController = new ConfirmationController {
      val sicSearchService: SicSearchService = mockSicSearchService
      val authConnector: AuthConnector = mockAuthConnector
      val messagesApi: MessagesApi = fakeApplication.injector.instanceOf[MessagesApi]
    }

    resetMocks()
  }

  val sessionId = "session-id-12345"

  val fakeRequestWithSessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(SessionKeys.sessionId -> sessionId)

  val sicCodeCode = "12345"
  val sicCodeDescription = "some description"
  val sicCode = SicCode(sicCodeCode, sicCodeDescription)
  val searchResults = SearchResults("testQuery", 1, List(sicCode))

  val sicStore = SicStore(
    sessionId,
    searchResults,
    Some(List(sicCode))
  )

  val sicStoreNoChoices = SicStore(sessionId, searchResults, None)
  val sicStoreEmptyChoiceList = SicStore(sessionId, searchResults, Some(List()))

  "show" should {

    "return a 200 when a SicStore is returned from mongo" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      showWithAuthorisedUser(controller.show, mockAuthConnector){
        result =>
          status(result) shouldBe 200
      }
    }

    "return a 303 when a SicStore ir not found in mongo" in new Setup {

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      showWithAuthorisedUser(controller.show, mockAuthConnector){
        result =>
          status(result) shouldBe 303
      }
    }
  }

  "submit" should {

    "return a 200 when the form field 'addAnother' is no" in new Setup {

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestWithSessionId.withFormUrlEncodedBody("addAnother" -> "no")

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, request){
        result =>
          status(result) shouldBe 200
      }
    }

    "return a 303 when the form field 'addAnother' is yes" in new Setup {

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestWithSessionId.withFormUrlEncodedBody("addAnother" -> "yes")

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      submitWithAuthorisedUser(controller.submit, mockAuthConnector, request){
        result =>
          status(result) shouldBe 303
      }
    }
  }

  "removeChoice" should {

    "return a 200 when the supplied sic code is removed" in new Setup {

      when(mockSicSearchService.removeChoice(any(), any())(any()))
        .thenReturn(Future.successful(true))

      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      requestWithAuthorisedUser(controller.removeChoice(sicCodeCode), mockAuthConnector, fakeRequestWithSessionId){
        result =>
          status(result) shouldBe OK
      }
    }
  }

  "withCurrentUsersChoices" should {

    "return a 303 and redirect to SicSearch when a SicStore does not exist" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(None))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Result = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/sic-search/enter-keywords")
    }

    "return a 303 and redirect to SicSearch when a SicStore does exist but does not contain any choices" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List())))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Result = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/sic-search/enter-keywords")
    }

    "return a 200 when a SicStore does exist and the choices list is populated" in new Setup {
      when(mockSicSearchService.retrieveChoices(any())(any()))
        .thenReturn(Future.successful(Some(List(sicCode))))

      val f: List[SicCode] => Future[Result] = _ => Future.successful(Ok)
      val result: Result = controller.withCurrentUsersChoices(sessionId)(f)

      status(result) shouldBe OK
    }
  }
}

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

package helpers

import org.apache.pekko.util.Timeout
import helpers.auth.AuthHelpers
import helpers.mocks.WSHTTPMock
import org.mockito.Mockito.reset
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.{HeaderNames, HttpProtocol, MimeTypes, Status}
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.Future
import scala.concurrent.duration._

trait UnitTestSpec
    extends PlaySpec
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with HeaderNames
    with Status
    with MimeTypes
    with HttpProtocol
    with DefaultAwaitTimeout
    with ResultExtractors
    with Writeables
    with EssentialActionCaller
    with RouteInvokers
    with FutureAwaits
    with MockedComponents
    with JsonFormValidation
    with PatienceConfiguration
    with IntegrationPatience {

  override implicit def defaultAwaitTimeout: Timeout = 5.seconds

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val request: Request[_] = FakeRequest()

  private def resetMocks(): Unit = {
    reset(
      mockSicSearchService,
      mockICLConnector,
      mockSicStoreRepository,
      mockAuditConnector,
      mockAuthConnector,
      mockWSHttp,
      mockServicesConfig
    )
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    resetMocks()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    resetMocks()
  }

  object AuthHelpers extends AuthHelpers {
    override val authConnector: AuthConnector = mockAuthConnector
  }

  def awaitAndAssert[T](func: => Future[T])(assertions: T => Assertion): Assertion =
    assertions(await(func))

  def assertFutureResult(func: => Future[Result])(assertions: Future[Result] => Assertion): Assertion =
    assertions(func)

  implicit class FakeRequestImps[T <: AnyContent](fakeRequest: FakeRequest[T]) {

    import uk.gov.hmrc.http.HeaderNames

    def withSessionId(sessionId: String): FakeRequest[T] =
      fakeRequest.withHeaders(SessionKeys.sessionId -> sessionId, HeaderNames.xSessionId -> sessionId)
  }

  trait CodeMocks extends WSHTTPMock with MockedComponents with MockitoSugar

  object MockAuthRedirects {
    val loginURL = "/test/login"
  }

  def assertAndAwait[T](testMethod: => Future[T])(assertions: T => Assertion): Assertion =
    assertions(await(testMethod))
}

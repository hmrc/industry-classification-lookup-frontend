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

package helpers.auth

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

trait AuthHelpers {
  val authConnector: AuthConnector

  def mockAuthorisedUser(future: Future[Unit]) {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(future)
  }

  def showWithUnauthorisedUser(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.failed(MissingBearerToken("")))
    test(action(request))
  }

  def showWithAuthorisedUser(action: Action[AnyContent], request: Request[AnyContent])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    test(action(request))
  }

  def submitWithUnauthorisedUser(action: Action[AnyContent], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.failed(MissingBearerToken("")))
    test(action(request))
  }

  def submitWithAuthorisedUser(action: Action[AnyContent], request: FakeRequest[AnyContentAsFormUrlEncoded])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    test(action(request))
  }

  def postWithAuthorisedUser(action: Action[JsValue], request: FakeRequest[JsValue])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.successful({}))
    test(action(request))
  }

  def postWithUnauthorisedUser(action: Action[JsValue], request: FakeRequest[JsValue])(test: Future[Result] => Any) {
    mockAuthorisedUser(Future.failed(MissingBearerToken("")))
    test(action(request))
  }

  def requestWithAuthorisedUser[T <: AnyContent](action: Action[AnyContent], request: FakeRequest[T])(test: Future[Result] => Any) {
    when(authConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(()))
    test(action(request))
  }
}

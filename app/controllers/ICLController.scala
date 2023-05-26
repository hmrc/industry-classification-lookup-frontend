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

import auth.SicSearchExternalURLs
import config.Logging
import controllers.action.ICLLanguageSupport
import models.SicCodeChoice
import models.setup.{Identifiers, JourneyData}
import play.api.libs.json._
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract class ICLController @Inject()(mcc: MessagesControllerComponents
                                      )(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with AuthorisedFunctions with ICLLanguageSupport with SicSearchExternalURLs with Logging {

  val journeyService: JourneyService
  val sicSearchService: SicSearchService

  def userAuthorised(api: Boolean = false)(body: => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    authorised() {
      body
    }.recover(if (api) apiAuthErrorHandling() else authErrorHandling())
  }

  def authErrorHandling()(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      infoLog("[ICLController][authErrorHandling] session is inactive; redirecting to login")
      Redirect(loginURL)
    case e: AuthorisationException =>
      errorLog("[ICLController][authErrorHandling] Unexpected auth exception", e)
      InternalServerError
  }

  def apiAuthErrorHandling()(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession =>
      infoLog("[ICLController][apiAuthErrorHandling] session is inactive; request forbidden")
      Forbidden
    case _: NotFoundException =>
      infoLog("[ICLController][apiAuthErrorHandling] API returned Not Found")
      Forbidden
    case e: AuthorisationException =>
      errorLog("[ICLController][apiAuthErrorHandling] Unexpected auth exception", e)
      InternalServerError
  }

  def withSessionId(f: => String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    hc.sessionId.fold[Future[Result]](Future.successful(BadRequest("SessionId is missing from request")))(sessionId => f(sessionId.value))
  }

  def withJsBody[T](reads: Reads[T])(f: T => Future[Result])(implicit request: Request[JsValue]): Future[Result] = {
    Try(request.body.validate[T](reads)) match {
      case Success(JsSuccess(payload, _)) =>
        infoLog("[ICLController][withJsBody] Journey initialisation successful")
        f(payload)
      case Success(JsError(errs)) =>
        warnLog("[ICLController][withJsBody] Bad request" )
        Future(BadRequest(Json.prettyPrint(JsError.toJson(errs))))
      case Failure(e) =>
        errorLog("[ICLController][withJsBody] Request failed", e)
        Future(BadRequest(s"Could not parse body due to ${e.getMessage}"))
    }
  }

  def hasJourney(identifiers: Identifiers)(f: => JourneyData => Future[Result])(implicit request: Request[_]): Future[Result] = {
    journeyService.getJourney(identifiers) flatMap { journeyData =>
      f(journeyData)
    } recoverWith {
      case err =>
        errorLog(s"[ICLController][hasJourney] - msg: $err ${err.getMessage}", err)
        throw err
    }
  }

  def withJourney(journeyId: String)(f: => JourneyData => Future[Result])(implicit req: Request[_]): Future[Result] = {
    withSessionId { sessionId =>
      hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
        f(journeyData)
      }.recoverWith {
        case _ => Future.successful(Redirect(controllers.test.routes.TestSetupController.show(journeyId)))
      }
    }
  }

  private[controllers] def withCurrentUsersChoices(identifiers: Identifiers)(f: List[SicCodeChoice] => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    sicSearchService.retrieveChoices(identifiers.journeyId) flatMap {
      case Some(choices) => choices match {
        case Nil => Future.successful(Redirect(controllers.routes.ChooseActivityController.show(identifiers.journeyId)))
        case listOfChoices => f(listOfChoices)
      }
      case None => Future.successful(Redirect(controllers.routes.ChooseActivityController.show(identifiers.journeyId)))
    }
  }

  private[controllers] def getLang(implicit request: Request[_]) = {
    request.cookies.get(messagesApi.langCookieName) match {
      case Some(langCookie) => langCookie.value
      case _ => "en"
    }
  }
}

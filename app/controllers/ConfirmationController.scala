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

import javax.inject.{Inject, Singleton}

import auth.SicSearchRegime
import config.FrontendAuthConnector
import forms.ConfirmationForm
import models.Confirmation
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import repositories.models.SicCode
import services.SicSearchService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ConfirmationControllerImpl @Inject()(val messagesApi: MessagesApi,
                                           val sicSearchService: SicSearchService,
                                           val authConnector: FrontendAuthConnector) extends ConfirmationController

trait ConfirmationController extends Actions with I18nSupport {

  val sicSearchService: SicSearchService

  val show: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit request =>
      implicit user =>
        withSessionId { sessionId =>
          withCurrentUsersChoices(sessionId){ choices =>
            Future.successful(Ok(views.html.pages.confirmation(ConfirmationForm.form, choices)))
          }
        }
  }

  val submit: Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit request =>
      implicit user =>
        withSessionId { sessionId =>
          withCurrentUsersChoices(sessionId){ choices =>
            if(choices.size >= 4){
              Future.successful(Ok("End of journey"))
            } else {
              ConfirmationForm.form.bindFromRequest().fold(
                errors => Future.successful(BadRequest(views.html.pages.confirmation(errors, choices))),
                success =>
                  success.addAnother match {
                    case Confirmation.YES => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
                    case Confirmation.NO => Future.successful(Ok("End of journey"))
                  }
              )
            }
          }
        }
  }

  def removeChoice(sicCode: String): Action[AnyContent] = AuthorisedFor(taxRegime = new SicSearchRegime, pageVisibility = GGConfidence).async {
    implicit request =>
      implicit user =>
        withSessionId{ sessionId =>
          sicSearchService.removeChoice(sessionId, sicCode) flatMap { _ =>
            withCurrentUsersChoices(sessionId){ choices =>
              Future.successful(Ok(views.html.pages.confirmation(ConfirmationForm.form, choices)))
            }
          }
        }
  }

  private[controllers] def withCurrentUsersChoices(sessionId: String)(f: List[SicCode] => Future[Result]): Future[Result] = {
    sicSearchService.retrieveSicStore(sessionId) flatMap {
      case Some(sicStore) => sicStore.choices match {
        case Some(Nil) => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
        case Some(listOfChoices) => f(listOfChoices)
      }
      case None => Future.successful(Redirect(controllers.routes.SicSearchController.show()))
    }
  }
}

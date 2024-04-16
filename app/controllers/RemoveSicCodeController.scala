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

import config.AppConfig
import forms.RemoveSicCodeForm
import models.SicCodeChoice
import play.api.data.Form
import play.api.mvc._
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.removeActivityConfirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveSicCodeController @Inject() (
  mcc: MessagesControllerComponents,
  val sicSearchService: SicSearchService,
  val journeyService: JourneyService,
  val authConnector: AuthConnector,
  view: removeActivityConfirmation
)(implicit ec: ExecutionContext, val appConfig: AppConfig)
    extends ICLController(mcc) {

  def confirmationForm(description: String): Form[String] = RemoveSicCodeForm.form(description)

  private def withSicCodeChoice(journeyId: String, codes: List[SicCodeChoice], sicCode: String)(
    f: SicCodeChoice => Future[Result]
  ): Future[Result] =
    codes.find(_.code == sicCode).fold(
      Future.successful(Redirect(controllers.routes.ChooseActivityController.show(journeyId, Some(true))))
    )(f)

  def show(journeyId: String, sicCode: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withJourney(journeyId) { journey =>
        withCurrentUsersChoices(journey.identifiers) { codes =>
          withSicCodeChoice(journeyId, codes, sicCode) { code =>
            Future.successful(Ok(view(journeyId, confirmationForm(code.getDescription), code)))
          }
        }
      }
    }
  }

  def submit(journeyId: String, sicCode: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withJourney(journeyId) { journeyData =>
        withCurrentUsersChoices(journeyData.identifiers) { codes =>
          withSicCodeChoice(journeyId, codes, sicCode) { code =>
            confirmationForm(code.getDescription).bindFromRequest().fold(
              errors => Future.successful(BadRequest(view(journeyId, errors, code))),
              {
                case "yes" =>
                  sicSearchService.removeChoice(journeyData.identifiers.journeyId, sicCode) map { _ =>
                    Redirect(controllers.routes.ConfirmationController.show(journeyId))
                  }
                case "no" => Future.successful(Redirect(controllers.routes.ConfirmationController.show(journeyId)))
              }
            )
          }
        }
      }
    }
  }
}

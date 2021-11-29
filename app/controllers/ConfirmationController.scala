/*
 * Copyright 2021 HM Revenue & Customs
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
import models.setup.messages.Summary
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import views.html.pages.confirmation

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmationController @Inject()(mcc: MessagesControllerComponents,
                                       val sicSearchService: SicSearchService,
                                       val journeyService: JourneyService,
                                       val authConnector: AuthConnector,
                                       view: confirmation
                                      )(implicit ec: ExecutionContext,
                                        val appConfig: AppConfig) extends ICLController(mcc) {

  def show(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journeyData =>
          withCurrentUsersChoices(journeyData.identifiers) { choices =>
            val summary = journeyData.journeySetupDetails.customMessages.flatMap(_.summary).getOrElse(Summary(None, None, None))
            Future.successful(Ok(view(journeyId, choices, summaryContent = summary)))
          }
        }
      }
  }

  def submit(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journeyData =>
          withCurrentUsersChoices(journeyData.identifiers) { choices =>
            if (choices.size <= 4) {
              journeyService.getRedirectUrl(journeyData.identifiers) map { url =>
                Redirect(url)
              }
            } else {
              val amountToRemove = (choices.size - 4).toString
              Future.successful(BadRequest(view(journeyId, choices, Some(Seq(amountToRemove)))))
            }
          }
        }
      }
  }
}

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
                                       view: confirmation)
                                      (implicit ec: ExecutionContext, val appConfig: AppConfig) extends ICLController(mcc) {

  private val maxChoices = 4

  def show(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journeyData =>
          withCurrentUsersChoices(journeyData.identifiers) { choices =>
            val customMessages = journeyData.journeySetupDetails.customMessages

            val langCookieValue = request.cookies.get(messagesApi.langCookieName).map(_.value)
            val maybeSummaryContent = langCookieValue match {
              case Some("cy") => customMessages.flatMap(_.summaryCy)
              case _ => customMessages.flatMap(_.summary)
            }

            Future.successful(Ok(view(
              journeyId, choices, summaryContent = maybeSummaryContent.getOrElse(Summary(None, None, None))
            )))
          }
        }
      }
  }

  def submit(journeyId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journeyData =>
          withCurrentUsersChoices(journeyData.identifiers) { choices =>
            if (choices.size <= maxChoices) {
              journeyService.getRedirectUrl(journeyData.identifiers) map { url =>
                Redirect(url)
              }
            } else {
              // Used to tell the user how many choices they must remove to proceed
              val amountToRemove = (choices.size - maxChoices).toString
              Future.successful(BadRequest(view(journeyId, choices, errorArgs = Some(Seq(amountToRemove)))))
            }
          }
        }
      }
  }
}

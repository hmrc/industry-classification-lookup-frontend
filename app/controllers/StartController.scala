/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartController @Inject()(mcc: MessagesControllerComponents,
                                val servicesConfig: ServicesConfig,
                                val authConnector: AuthConnector,
                                val sicSearchService: SicSearchService,
                                val journeyService: JourneyService
                               )(implicit ec: ExecutionContext)
  extends ICLController(mcc) {

  def startJourney(jId: String): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(jId) { journeyData =>
          if (journeyData.journeySetupDetails.sicCodes.isEmpty) {
            Future.successful(Redirect(routes.ChooseActivityController.show(jId)))
          } else {
            Future.successful(Redirect(routes.ConfirmationController.show(jId)))
          }
        }
      }
  }
}

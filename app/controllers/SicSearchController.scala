/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import auth.SicSearchExternalURLs
import config.AppConfig
import forms.sicsearch.SicSearchForm
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class SicSearchControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val servicesConfig: ServicesConfig,
                                        val appConfig: AppConfig,
                                        val sicSearchService: SicSearchService,
                                        val journeyService: JourneyService,
                                        val authConnector: AuthConnector) extends SicSearchController with SicSearchExternalURLs

trait SicSearchController extends ICLController {

  val sicSearchService : SicSearchService

  val show: Action[AnyContent] = Action.async {
    implicit request =>
      authorised {
        withJourney { _ =>
          Future.successful(Ok(views.html.pages.sicsearch(SicSearchForm.form)))
        }
      }
  }

  val submit: Action[AnyContent] = Action.async {
    implicit request =>
      authorised {
        withJourney { journey =>
          SicSearchForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(views.html.pages.sicsearch(errors))),
            form => sicSearchService.search(journey.sessionId, form.sicSearch, journey.name, None).map {
              case 0 => Ok(views.html.pages.sicsearch(SicSearchForm.form, Some(form.sicSearch)))
              case 1 => Redirect(routes.ConfirmationController.show())
              case _ => Redirect(routes.ChooseActivityController.show())
            }
          )
        }
      }
  }
}

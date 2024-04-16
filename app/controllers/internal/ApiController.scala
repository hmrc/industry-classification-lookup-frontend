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

package controllers.internal

import config.AppConfig
import controllers.ICLController
import models.setup.{Identifiers, JourneyData}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ApiController @Inject() (
  mcc: MessagesControllerComponents,
  val journeyService: JourneyService,
  val sicSearchService: SicSearchService,
  val authConnector: AuthConnector
)(implicit ec: ExecutionContext, val appConfig: AppConfig)
    extends ICLController(mcc) {

  def journeyInitialisation(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised() {
      withSessionId { sessionId =>
        withJsBody[JourneyData](JourneyData.initialRequestReads(sessionId)) { journeyData =>
          journeyService.initialiseJourney(journeyData, getLang).map(Ok(_))
        }
      }
    }
  }

  def fetchResults(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    authorised() {
      withSessionId { sessionId =>
        hasJourney(Identifiers(journeyId, sessionId)) { _ =>
          sicSearchService.retrieveChoices(journeyId) map {
            case Some(choices) => Ok(Json.obj("sicCodes" -> Json.toJson(choices)))
            case None          => NotFound
          }
        }
      }
    }
  }
}

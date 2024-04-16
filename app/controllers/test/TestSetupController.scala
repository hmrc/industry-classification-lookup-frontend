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

package controllers.test

import config.AppConfig
import controllers.ICLController
import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.voa.play.form.ConditionalMappings.{isEqual, mandatoryIf}
import views.html.test.SetupJourneyView

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TestSetupController @Inject() (
  mcc: MessagesControllerComponents,
  val journeyService: JourneyService,
  val sicSearchService: SicSearchService,
  val authConnector: AuthConnector,
  view: SetupJourneyView
)(implicit ec: ExecutionContext, val appConfig: AppConfig)
    extends ICLController(mcc) {

  val journeySetupForm: Form[JourneySetup] = {
    def journeySetupApply(
      dataSet: String = JourneyData.ONS,
      queryParser: Option[Boolean] = None,
      queryBooster: Option[Boolean] = None,
      amountOfResults: Int = 50
    ): JourneySetup = new JourneySetup(dataSet, queryParser, queryBooster, amountOfResults, None)

    def journeySetupUnapply(arg: JourneySetup): Option[(String, Option[Boolean], Option[Boolean], Int)] =
      Some((arg.dataSet, arg.queryParser, arg.queryBooster, arg.amountOfResults))

    Form(
      mapping(
        "dataSet"         -> nonEmptyText.verifying(dSet => JourneyData.dataSets.contains(dSet)),
        "queryParser"     -> optional(boolean),
        "booster"         -> mandatoryIf(isEqual("queryParser", "false"), boolean),
        "amountOfResults" -> number(1)
      )(journeySetupApply)(journeySetupUnapply)
    )
  }

  def show(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withSessionId { sessionId =>
        hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
          Future.successful(Ok(view(journeyId, journeySetupForm.fill(journeyData.journeySetupDetails))))
        }
      }
    }
  }

  def submit(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withSessionId { sessionId =>
        hasJourney(Identifiers(journeyId, sessionId)) { journeyData =>
          journeySetupForm.bindFromRequest().fold(
            errors => Future.successful(BadRequest(view(journeyId, errors))),
            validJourney =>
              journeyService.updateJourneyWithJourneySetup(journeyData.identifiers, validJourney).map(_ =>
                Redirect(controllers.routes.ChooseActivityController.show(journeyId))
              )
          )
        }
      }
    }
  }

  val testSetup: Action[AnyContent] = Action.async { implicit request =>
    userAuthorised(api = true) {
      withSessionId { sessionId =>
        val journeyId: String = sessionId
        val journeyData: JourneyData = JourneyData(
          identifiers = Identifiers(journeyId, sessionId),
          redirectUrl = s"/sic-search/test-only/$journeyId/end-of-journey",
          journeySetupDetails = JourneySetup(),
          lastUpdated = Instant.now()
        )
        journeyService.initialiseJourney(journeyData, getLang).map(_ =>
          Redirect(controllers.test.routes.TestSetupController.show(journeyId))
        )
      }
    }
  }

  def endOfJourney(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withSessionId { sessionId =>
        hasJourney(Identifiers(journeyId, sessionId)) { _ =>
          sicSearchService.retrieveChoices(journeyId) map { choices =>
            Ok("End of Journey" + Json.prettyPrint(Json.toJson(choices)))
          }
        }
      }
    }
  }
}

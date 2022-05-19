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

package services

import javax.inject.{Inject, Singleton}
import models.SicCode
import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.libs.json.{JsValue, Json}
import repositories._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class JourneyService @Inject()(journeyDataRepository: JourneyDataRepository,
                               val sicSearchService: SicSearchService) {

  def initialiseJourney(journeyData: JourneyData)(implicit hc: HeaderCarrier): Future[JsValue] = {
    for {
      res <- journeyDataRepository.upsertJourney(journeyData) map { _ =>
        Json.obj(
          "journeyStartUri" -> s"/sic-search/${journeyData.identifiers.journeyId}/start-journey",
          "fetchResultsUri" -> s"/internal/${journeyData.identifiers.journeyId}/fetch-results"
        )
      }
      sicCodes = journeyData.journeySetupDetails.sicCodes map (SicCode(_, ""))
      _ <- sicSearchService.lookupSicCodes(journeyData, sicCodes.toList)
    } yield res
  }

  def updateJourneyWithJourneySetup(identifiers: Identifiers, journeySetupDetails: JourneySetup): Future[JourneySetup] = {
    journeyDataRepository.updateJourneySetup(identifiers, journeySetupDetails)
  }

  def getJourney(identifiers: Identifiers): Future[JourneyData] = {
    journeyDataRepository.retrieveJourneyData(identifiers)
  }

  def getRedirectUrl(identifiers: Identifiers): Future[String] = {
    journeyDataRepository.retrieveJourneyData(identifiers) map (_.redirectUrl)
  }
}
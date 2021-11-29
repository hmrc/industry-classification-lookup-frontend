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

package services

import config.Logging
import connectors.ICLConnector

import javax.inject.{Inject, Singleton}
import models._
import models.setup.JourneyData
import play.api.Logger
import repositories.SicStoreRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicSearchService @Inject()(val iCLConnector: ICLConnector,
                                 sicStoreRepository: SicStoreRepository) extends Logging {

  def search(journeyData: JourneyData, query: String, sector: Option[String] = None)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    if (isLookup(query)) {
      lookupSicCodes(journeyData, List(SicCode(query, "")))
    } else {
      searchQuery(journeyData, query, sector)
    }
  }

  def retrieveSearchResults(journeyId: String)(implicit ec: ExecutionContext): Future[Option[SearchResults]] = {
    sicStoreRepository.retrieveSicStore(journeyId).map(_.flatMap(_.searchResults))
  }

  def retrieveChoices(journeyId: String)(implicit ec: ExecutionContext): Future[Option[List[SicCodeChoice]]] = {
    sicStoreRepository.retrieveSicStore(journeyId).map(_.flatMap(_.choices))
  }

  def insertChoices(journeyId: String, sicCodes: List[SicCodeChoice])(implicit ec: ExecutionContext): Future[Boolean] =
    sicStoreRepository.insertChoices(journeyId, sicCodes)

  def removeChoice(journeyId: String, sicCodeToRemove: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    sicStoreRepository.removeChoice(journeyId, sicCodeToRemove)
  }

  def lookupSicCodes(journeyData: JourneyData, selectedCodes: List[SicCode])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    def fiteredListOfSicCodeChoice(sicCodesUnfiltered: List[SicCode], groups: Map[String, List[SicCode]]): List[SicCodeChoice] = {
      sicCodesUnfiltered map { sic =>
        SicCodeChoice(sic, groups.get(sic.sicCode).fold(List.empty[String])(nSicCodes =>
          nSicCodes.filterNot(sicCode => sicCode == sic || sicCode.description.isEmpty).map(_.description))
        )
      }
    }

    if (selectedCodes.isEmpty) {
      Future.successful(0)
    } else {
      for {
        oSicCode <- iCLConnector.lookup(getCommaSeparatedCodes(selectedCodes))
        groups = selectedCodes.groupBy(_.sicCode)
        filteredCodes = fiteredListOfSicCodeChoice(oSicCode, groups)
        res <- if (oSicCode.nonEmpty) {
          insertChoices(journeyData.identifiers.journeyId, filteredCodes) map (_ => 1)
        } else {
          Future.successful(0)
        }
      } yield res
    }
  }

  private[services] def getCommaSeparatedCodes(sicCodes: List[SicCode]): String = {
    sicCodes.groupBy(_.sicCode).keys.mkString(",")
  }

  private[services] def searchQuery(journeyData: JourneyData, query: String, sector: Option[String] = None)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    (for {
      oSearchResults <- iCLConnector.search(query, journeyData.journeySetupDetails, sector)
      sectorObject = sector.flatMap(sicCode => oSearchResults.sectors.find(_.code == sicCode))
      searchResults = sectorObject.fold(oSearchResults)(s => oSearchResults.copy(currentSector = Some(s)))
      _ <- sicStoreRepository.upsertSearchResults(journeyData.identifiers.journeyId, searchResults) flatMap { res =>
        if (searchResults.numFound == 1) lookupSicCodes(journeyData, searchResults.results) else Future.successful(res)
      }
    } yield searchResults.numFound) recover {
      case e =>
        logger.error(s"[SicSearchService] [searchQuery] Exception encountered when attempting to fetch results from ICL ${e.getMessage}")
        0
    }
  }

  private[services] def isLookup(query: String): Boolean = query.trim.matches("^(\\d){5}$")
}

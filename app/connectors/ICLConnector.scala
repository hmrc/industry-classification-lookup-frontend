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

package connectors

import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.setup.JourneySetup
import models.{SearchResults, SicCode}
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class ICLConnector @Inject()(appConfig: AppConfig,
                             http: HttpClient) {

  lazy val ICLUrl: String = appConfig.industryClassificationLookupBackend

  def lookup(sicCode: String)(implicit hc: HeaderCarrier): Future[List[SicCode]] = {
    http.GET[HttpResponse](s"$ICLUrl/industry-classification-lookup/lookup/$sicCode") map { resp =>
      if (resp.status == NO_CONTENT) List.empty[SicCode] else Json.fromJson[List[SicCode]](resp.json).getOrElse(List.empty[SicCode])
    } recover {
      case e: HttpException =>
        Logger.error(s"[Lookup] Looking up sic code : $sicCode returned a ${e.responseCode}")
        throw e
      case e: Throwable =>
        Logger.error(s"[Lookup] Looking up sic code : $sicCode has thrown a non-http exception")
        throw e
    }
  }

  def search(query: String, journeySetup: JourneySetup, sector: Option[String] = None)(implicit hc: HeaderCarrier): Future[SearchResults] = {
    implicit val reads: Reads[SearchResults] = SearchResults.readsWithQuery(query)
    val sectorFilter = sector.fold("")(s => s"&sector=$s")
    val constructUrlParameters = s"query=$query" +
      s"&pageResults=${journeySetup.amountOfResults}$sectorFilter" +
      s"&queryParser=${journeySetup.queryParser.getOrElse(false)}" +
      s"&queryBoostFirstTerm=${journeySetup.queryBooster.getOrElse(false)}" +
      s"&indexName=${journeySetup.dataSet}"

    http.GET[SearchResults](s"$ICLUrl/industry-classification-lookup/search?$constructUrlParameters") recover {
      case e: HttpException =>
        Logger.error(s"[Search] Searching using query : $query returned a ${e.responseCode}")
        SearchResults(
          query, 0, List(), List())
      case e =>
        Logger.error(s"[Search] Searching using query : $query has thrown a non-http exception")
        throw e
    }
  }
}

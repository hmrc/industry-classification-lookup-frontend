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

package connectors

import config.{AppConfig, Logging}
import models.setup.JourneySetup
import models.{SearchResults, SicCode}
import play.api.http.Status._
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpReads.Implicits
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ICLConnector @Inject()(appConfig: AppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {

  lazy val ICLUrl: String = appConfig.industryClassificationLookupBackend

  def lookup(sicCode: String)(implicit hc: HeaderCarrier): Future[List[SicCode]] = {
    http.get(url"$ICLUrl/industry-classification-lookup/lookup/$sicCode")
      .execute
      .map { resp =>
        resp.status match {
          case OK => Json.fromJson[List[SicCode]](resp.json).getOrElse(Nil)
          case NO_CONTENT => Nil
          case status =>
            logger.error(s"[Lookup] Looking up sic code: $sicCode returned a $status")
            throw new InternalServerException(s"[Lookup] Looking up sic code: $sicCode returned a $status")
        }
      } recover {
        case e: Throwable =>
          logger.error(s"[Lookup] Looking up sic code: $sicCode has thrown a non-http exception")
          throw e
      }
    }

  def search(query: String, journeySetup: JourneySetup, sector: Option[String] = None, lang: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SearchResults] = {
    implicit val reads: Reads[SearchResults] = SearchResults.readsWithQuery(query)
    val sectorFilter = sector.map(s => List("sector" -> s)).getOrElse(Nil)

    val filters = Seq(
      "query" -> query,
      "queryParser" -> s"${journeySetup.queryParser.getOrElse(false)}",
      "pageResults" -> s"${journeySetup.amountOfResults}",
      "queryBoostFirstTerm" -> s"${journeySetup.queryBooster.getOrElse(false)}",
      "indexName" -> journeySetup.dataSet,
      "lang" -> lang
    ) ++ sectorFilter

    http.get(url"$ICLUrl/industry-classification-lookup/search")
      .transform(_.withQueryStringParameters(filters: _*))
      .execute[SearchResults](Implicits.readFromJson, ec)
      .recover {
        case e: UpstreamErrorResponse =>
          logger.error(s"[Search] Searching using query : $query returned a ${e.statusCode}")
          SearchResults(query, numFound = 0, results = Nil, sectors = Nil)
        case e =>
          logger.error(s"[Search] Searching using query : $query has thrown a non-http exception")
          throw e
      }
  }

}

/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Inject

import config.WSHttp
import models.{SearchResults, SicCode}
import play.api.Logger
import play.api.libs.json.Reads
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpGet}
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ICLConnectorImpl @Inject()(config : ServicesConfig) extends ICLConnector {
  val http: WSHttp = WSHttp
  val ICLUrl: String = config.baseUrl("industry-classification-lookup")
}

trait ICLConnector {

  val http: HttpGet
  val ICLUrl: String

  def lookup(sicCode: String)(implicit hc: HeaderCarrier): Future[Option[SicCode]] = {
    http.GET[SicCode](s"$ICLUrl/industry-classification-lookup/lookup/$sicCode") map {
      Some.apply
    } recover {
      case e: HttpException =>
        Logger.error(s"[Lookup] Looking up sic code : $sicCode returned a ${e.responseCode}", e)
        None
      case e: Throwable =>
        Logger.error(s"[Lookup] Looking up sic code : $sicCode has thrown a non-http exception", e)
        throw e
    }
  }

  def search(query: String)(implicit hc: HeaderCarrier): Future[Option[SearchResults]] = {
    implicit val reads: Reads[SearchResults] = SearchResults.readsWithQuery(query)
    http.GET[SearchResults](s"$ICLUrl/industry-classification-lookup/search?query=$query&pageResults=10") map {
      Some.apply
    } recover {
      case e: HttpException =>
        Logger.error(s"[Search] Searching using query : $query returned a ${e.responseCode}", e)
        None
      case e: Throwable =>
        Logger.error(s"[Search] Searching using query : $query has thrown a non-http exception", e)
        throw e
    }
  }
}

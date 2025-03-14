/*
 * Copyright 2024 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathMatching}
import helpers.ClientSpec
import models.setup.JourneySetup
import models.{SearchResults, Sector, SicCode}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global

class ICLConnectorISpec extends ClientSpec {

  val sicCode = "12345"
  val sicCodeResult: SicCode = SicCode(sicCode, "some description", "some description")
  val lookupUrl: String = s"/industry-classification-lookup/lookup/$sicCode"

  val query: String = "test query"
  val zeroResults: SearchResults = SearchResults(query, 0, List(), List())
  val searchResults: SearchResults = SearchResults(query, 1, List(SicCode("12345", "some description", "some description")), List(Sector("A", "Example of a business sector", "Cy business sector", 1)))
  val sector: String = "B"
  val journeySetup: JourneySetup = JourneySetup(dataSet = "foo", queryBooster = None, amountOfResults = 5)
  val lang: String = "en"
  val searchUrl: String = "/industry-classification-lookup/search*"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val connector: ICLConnector = app.injector.instanceOf[ICLConnector]

  "lookup" should {
    "return a sic code case class matching the code provided" in {
      stubGet(lookupUrl, OK, Some(Json.stringify(Json.toJson[List[SicCode]](List(sicCodeResult)))))

      val res = await(connector.lookup(sicCode))

      res mustBe List(sicCodeResult)
    }
    "return an empty list when ICL returns NO_CONTENT" in {
      stubGet(lookupUrl, NO_CONTENT, None)

      val res = await(connector.lookup(sicCode))

      res mustBe Nil
    }
    "throw the exception when the future recovers an the exception is http related" in {
      stubGet(lookupUrl, INTERNAL_SERVER_ERROR, None)

      intercept[InternalServerException](await(connector.lookup(sicCode)))
    }
  }

  "search" should {
    "return a SearchResults case class when one is returned from ICL" in {
      stubFor(get(urlPathMatching(searchUrl))
        .willReturn(aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(searchResults)))))

      val res = await(connector.search(query, journeySetup, lang = lang))

      res mustBe searchResults
    }

    "return a SearchResults case class when a sector search is returned from ICL" in {
      stubFor(get(urlPathMatching(searchUrl))
        .willReturn(aResponse().withStatus(OK).withBody(Json.stringify(Json.toJson(searchResults)))))

      val res = await(connector.search(query, journeySetup, Some(sector), lang))

      res mustBe searchResults
    }

    "return 0 results when ICL returns NOT_FOUND" in {
      stubFor(get(urlPathMatching(searchUrl))
        .willReturn(aResponse().withStatus(NOT_FOUND)))

      val res = await(connector.search(query, journeySetup, lang = lang))

      res mustBe zeroResults
    }
  }
}

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

import helpers.UnitTestSpec
import helpers.mocks.MockAppConfig
import models.setup.JourneySetup
import models.{SearchResults, Sector, SicCode}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpException, HttpResponse, NotFoundException}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ICLConnectorSpec extends UnitTestSpec with MockAppConfig {

  val iCLUrl = "http://localhost:12345"

  class Setup extends CodeMocks {
    val connector: ICLConnector = new ICLConnector(
      appConfig = mockConfig,
      http = mockWSHttp
    ) {
      override lazy val ICLUrl: String = iCLUrl
    }
  }

  "lookup" should {

    val sicCode = "12345"
    val sicCodeResult = SicCode(sicCode, "some description")

    val lookupUrl = s"$iCLUrl/industry-classification-lookup/lookup/$sicCode"

    "return a sic code case class matching the code provided" in new Setup {
      mockHttpGet[HttpResponse](lookupUrl).thenReturn(Future.successful(HttpResponse(200, Some(Json.toJson[List[SicCode]](List(sicCodeResult))))))

      awaitAndAssert(connector.lookup(sicCode)) {
        _ mustBe List(sicCodeResult)
      }
    }

    "return an empty list when ICL returns a 204" in new Setup {
      mockHttpGet[HttpResponse](lookupUrl).thenReturn(Future.successful(HttpResponse(204)))

      awaitAndAssert(connector.lookup(sicCode)) {
        _ mustBe Nil
      }
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.lookup(sicCode)))
      result.getMessage mustBe "something went wrong"
    }

    "throw the exception when the future recovers an the exception is http related" in new Setup {
      mockHttpGet[SicCode](lookupUrl).thenReturn(Future.failed(new HttpException("Not Found", 404)))

      val result: HttpException = intercept[HttpException](await(connector.lookup(sicCode)))
      result.getMessage mustBe "Not Found"
      result.responseCode mustBe 404
    }
  }

  "search" should {

    val query = "test query"
    val zeroResults = SearchResults(query, 0, List(), List())
    val searchResults = SearchResults(query, 1, List(SicCode("12345", "some description")), List(Sector("A", "Example of a business sector", "Cy business sector", 1)))
    val sector = "B"
    val journeySetup = JourneySetup(dataSet = "foo", queryBooster = None, amountOfResults = 5)
    val searchUrl = s"$iCLUrl/industry-classification-lookup/search?query=$query" +
      s"&pageResults=${journeySetup.amountOfResults}" +
      s"&queryParser=${journeySetup.queryParser.getOrElse(false)}" +
      s"&queryBoostFirstTerm=${journeySetup.queryBooster.getOrElse(false)}" +
      s"&indexName=${journeySetup.dataSet}"
    val searchSectorUrl = s"$iCLUrl/industry-classification-lookup/search?query=$query" +
      s"&pageResults=${journeySetup.amountOfResults}" +
      s"&sector=$sector" +
      s"&queryParser=${journeySetup.queryParser.getOrElse(false)}" +
      s"&queryBoostFirstTerm=${journeySetup.queryBooster.getOrElse(false)}" +
      s"&indexName=${journeySetup.dataSet}"

    "return a SearchResults case class when one is returned from ICL" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.successful(searchResults))

      awaitAndAssert(connector.search(query, journeySetup)) {
        _ mustBe searchResults
      }
    }

    "return a SearchResults case class when a sector search is returned from ICL" in new Setup {
      mockHttpGet[SearchResults](searchSectorUrl).thenReturn(Future.successful(searchResults))

      awaitAndAssert(connector.search(query, journeySetup, Some(sector))) {
        _ mustBe searchResults
      }
    }

    "return 0 results when ICL returns a 404" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new NotFoundException("404")))

      awaitAndAssert(connector.search(query, journeySetup)) {
        _ mustBe zeroResults
      }
    }

    "throw the exception when the future recovers an the exception is not http related" in new Setup {
      mockHttpGet[SearchResults](searchUrl).thenReturn(Future.failed(new RuntimeException("something went wrong")))

      val result: RuntimeException = intercept[RuntimeException](await(connector.search(query, journeySetup)))
      result.getMessage mustBe "something went wrong"
    }
  }
}

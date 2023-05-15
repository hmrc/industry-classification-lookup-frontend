
package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathMatching}
import helpers.ClientSpec
import models.setup.JourneySetup
import models.{SearchResults, Sector, SicCode}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.ExecutionContext.Implicits.global

class ICLConnectorISpec extends ClientSpec {

  val sicCode = "12345"
  val sicCodeResult = SicCode(sicCode, "some description", "some description")
  val lookupUrl = s"/industry-classification-lookup/lookup/$sicCode"

  val query = "test query"
  val zeroResults = SearchResults(query, 0, List(), List())
  val searchResults = SearchResults(query, 1, List(SicCode("12345", "some description", "some description")), List(Sector("A", "Example of a business sector", "Cy business sector", 1)))
  val sector = "B"
  val journeySetup = JourneySetup(dataSet = "foo", queryBooster = None, amountOfResults = 5)
  val lang = "en"
  val searchUrl = "/industry-classification-lookup/search*"

  implicit val request = FakeRequest()
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val connector = app.injector.instanceOf[ICLConnector]

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

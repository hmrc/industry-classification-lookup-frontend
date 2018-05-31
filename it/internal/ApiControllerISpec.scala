/*
 * Copyright 2018 HM Revenue & Customs
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

package internal

import java.time.LocalDateTime

import helpers.ClientSpec
import models.setup.{Identifiers, JourneyData, JourneySetup}
import models.{SicCode, SicCodeChoice, SicStore}
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.FakeApplication
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult
import repositories.{JourneyDataMongoRepository, JourneyDataRepo, SicStoreMongoRepository, SicStoreRepo}

import scala.concurrent.ExecutionContext.Implicits.global

class ApiControllerISpec extends ClientSpec {

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "auditing.consumer.baseUri.host" -> s"$wiremockHost",
    "auditing.consumer.baseUri.port" -> s"$wiremockPort",
    "microservice.services.cachable.session-cache.host" -> s"$wiremockHost",
    "microservice.services.cachable.session-cache.port" -> s"$wiremockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.cachable.short-lived-cache.host" -> s"$wiremockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$wiremockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "microservice.services.auth.host" -> s"$wiremockHost",
    "microservice.services.auth.port" -> s"$wiremockPort"
  ))

  trait Setup {
    val sicStoreRepo: SicStoreMongoRepository = fakeApplication.injector.instanceOf[SicStoreRepo].repo
    val journeyRepo: JourneyDataMongoRepository = fakeApplication.injector.instanceOf[JourneyDataRepo].store

    def insertSicStore(sicStore: SicStore): WriteResult = await(sicStoreRepo.insert(sicStore))
    def insertJourney(journeyData: JourneyData): WriteResult = await(journeyRepo.insert(journeyData))

    await(sicStoreRepo.drop)
    await(journeyRepo.drop)
    await(sicStoreRepo.ensureIndexes)
    await(journeyRepo.ensureIndexes)

  }

  val journeyId: String = "test-journey-id"

  val initialiseJourneyUrl = "/internal/initialise-journey"
  val fetchResultsUrl = s"/internal/$journeyId/fetch-results"

  val setupJson = Json.parse(
    """
      |{
      |   "redirectUrl" : "/test/uri"
      |}
    """.stripMargin
  )

  "/internal/initialise-journey" should {
    "return an OK" when {
      "the json has been validated and the journey has been setup" in {
        setupUnauthorised()

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHeaders(HeaderNames.COOKIE -> getSessionCookie()).post(setupJson)) { res =>
          res.status mustBe OK
          assert(res.json.\("journeyStartUri").as[String].contains("/search-standard-industry-classification-codes"))
          assert(res.json.\("fetchResultsUri").as[String].contains("/fetch-results"))
        }
      }
    }

    "return a Bad Request" when {
      "there was a problem validating the input json" in {
        setupUnauthorised()

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHeaders(HeaderNames.COOKIE -> getSessionCookie()).post(Json.parse("""{"abc" : "xyz"}"""))) {
          _.status mustBe BAD_REQUEST
        }
      }

      "no session id could be found in request" in {
        setupUnauthorised()

        assertFutureResponse(buildClient(initialiseJourneyUrl).post(setupJson)) { res =>
          res.status mustBe BAD_REQUEST
        }
      }
    }
  }

  "/internal/journeyID/fetch-results" must {

    "return an OK" when {
      "the journey exists and there are selected sic codes" in new Setup {
        setupUnauthorised()

        val sessionIdValue = "test-session-id"
        val sessionId: String = getSessionCookie(sessionID = "test-session-id")

        val sicCodeChoices = List(SicCodeChoice(SicCode("12345", "test description"), Nil))
        val sicStore: SicStore = SicStore(sessionIdValue, "testJourney", "testDataSet", None, Some(sicCodeChoices))
        insertSicStore(sicStore)

        val journey: JourneyData = JourneyData(Identifiers(journeyId, sessionIdValue), "redirect-url", None, JourneySetup(), LocalDateTime.now())
        insertJourney(journey)

        assertFutureResponse(buildClient(fetchResultsUrl).withHeaders(HeaderNames.COOKIE -> sessionId).get()) { res =>
          res.status mustBe OK
          (res.json \ "sicCodes").as[List[SicCodeChoice]] mustBe sicCodeChoices
        }
      }
    }
    "return a Bad Request" when {
      "there is no session id in the request" in new Setup {
        setupUnauthorised()

        assertFutureResponse(buildClient(fetchResultsUrl).get()) { res =>
          res.status mustBe BAD_REQUEST
          res.body mustBe "SessionId is missing from request"
        }
      }
    }
    "return a Not Found" when {
      "there is no journey setup for the session" in new Setup {
        setupUnauthorised()

        val sessionIdValue = "test-session-id"
        val sessionId: String = getSessionCookie(sessionID = "test-session-id")

        assertFutureResponse(buildClient(fetchResultsUrl).withHeaders(HeaderNames.COOKIE -> sessionId).get()) { res =>
          res.status mustBe NOT_FOUND
        }
      }
      "there is a journey setup but no sic codes have been selected" in new Setup {
        setupUnauthorised()

        val sessionIdValue = "test-session-id"
        val sessionId: String = getSessionCookie(sessionID = "test-session-id")

        val journey: JourneyData = JourneyData(Identifiers(journeyId, sessionIdValue), "redirect-url", None, JourneySetup(), LocalDateTime.now())
        insertJourney(journey)

        assertFutureResponse(buildClient(fetchResultsUrl).withHeaders(HeaderNames.COOKIE -> sessionId).get()) { res =>
          res.status mustBe NOT_FOUND
        }
      }
    }
  }
}
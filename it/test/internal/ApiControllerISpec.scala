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

import helpers.{ClientSpec, CookieBaker}
import models.setup.messages.{CustomMessages, Summary}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import models.{SicCode, SicCodeChoice, SicStore}
import org.mongodb.scala.result.InsertOneResult
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.{JourneyDataRepository, SicStoreRepository}

import java.time.Instant

class ApiControllerISpec extends ClientSpec {

  implicit val request = FakeRequest()

  trait Setup {
    val sicStoreRepo: SicStoreRepository = app.injector.instanceOf[SicStoreRepository]
    val journeyRepo: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]

    def insertSicStore(sicStore: SicStore): InsertOneResult = await(sicStoreRepo.collection.insertOne(sicStore).toFuture())

    def insertJourney(journeyData: JourneyData) = await(journeyRepo.upsertJourney(journeyData))

    await(sicStoreRepo.collection.drop.toFuture())
    await(journeyRepo.collection.drop.toFuture())
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
      val regexJourneyStartUri = raw"/sic-search/(.+)/start-journey".r
      val regexFetchResultsUri = raw"/internal/(.+)/fetch-results".r

      "the json has been validated and the journey has been setup (without journey setup details)" in new Setup {
        setupSimpleAuthMocks()
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0
        await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0

        stubGet("/industry-classification-lookup/lookup/", 200, Some("{}"))

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHttpHeaders(
          HeaderNames.COOKIE -> CookieBaker.getSessionCookie(),
          "X-Session-Id" -> CookieBaker.defaultSessionId,
          HeaderNames.AUTHORIZATION -> "test"
        ).post(setupJson)) { res =>
          res.status mustBe OK
          val fetchResUri = res.json.\("fetchResultsUri").as[String]
          val journeyStartUri = res.json.\("journeyStartUri").as[String]
          assert(journeyStartUri.matches(regexJourneyStartUri.toString))
          assert(fetchResUri.matches(regexFetchResultsUri.toString))

          val journeyIdGenerated = regexFetchResultsUri.findFirstMatchIn(fetchResUri).get.group(1)

          await(journeyRepo.collection.countDocuments().toFuture()) mustBe 1
          val data = await(journeyRepo.retrieveJourneyData(Identifiers(journeyIdGenerated, CookieBaker.defaultSessionId)))
          data.redirectUrl mustBe "/test/uri"
          data.journeySetupDetails.queryParser mustBe None
          data.journeySetupDetails.queryBooster mustBe None
          data.journeySetupDetails.amountOfResults mustBe 50
          data.journeySetupDetails.customMessages mustBe None
          data.journeySetupDetails.sicCodes mustBe Seq.empty[String]
          await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0
        }
      }

      "the json has been validated and the journey has been setup with sic store data (with journey setup details)" in new Setup {
        val setupJsonWithDetails = Json.parse(
          """
            |{
            |   "redirectUrl" : "/test/uri",
            |   "journeySetupDetails": {
            |     "queryBooster": true,
            |     "amountOfResults": 200,
            |     "customMessages": {
            |       "summary": {
            |         "heading": "Some heading",
            |         "lead": "Some lead",
            |         "hint": "Some hint"
            |       },
            |       "summaryCy": {
            |         "heading": "Welsh heading",
            |         "lead": "Welsh lead",
            |         "hint": "Welsh hint"
            |       }
            |     },
            |     "sicCodes": ["12345", "67890"]
            |   }
            |}
          """.stripMargin
        )

        setupSimpleAuthMocks()
        val responseBody = Json.toJson(List(SicCode("12345", "desc one", "desc one"), SicCode("67890", "desc 2", "desc 2"))).toString()

        stubGet(s"/industry-classification-lookup/lookup/67890,12345", 200, Some(responseBody))
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0
        await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHttpHeaders(
          HeaderNames.COOKIE -> CookieBaker.getSessionCookie(),
          "X-Session-Id" -> CookieBaker.defaultSessionId,
          HeaderNames.AUTHORIZATION -> "test"
        ).post(setupJsonWithDetails)) { res =>
          res.status mustBe OK
          val fetchResUri = res.json.\("fetchResultsUri").as[String]
          val journeyStartUri = res.json.\("journeyStartUri").as[String]
          assert(journeyStartUri.matches(regexJourneyStartUri.toString))
          assert(fetchResUri.matches(regexFetchResultsUri.toString))

          val journeyIdGenerated = regexFetchResultsUri.findFirstMatchIn(fetchResUri).get.group(1)

          await(journeyRepo.collection.countDocuments().toFuture()) mustBe 1
          val data = await(journeyRepo.retrieveJourneyData(Identifiers(journeyIdGenerated, CookieBaker.defaultSessionId)))
          data.redirectUrl mustBe "/test/uri"
          data.journeySetupDetails.queryParser mustBe None
          data.journeySetupDetails.queryBooster mustBe Some(true)
          data.journeySetupDetails.amountOfResults mustBe 200
          data.journeySetupDetails.customMessages mustBe Some(CustomMessages(
            Some(Summary(heading = Some("Some heading"), lead = Some("Some lead"), hint = Some("Some hint"))),
            Some(Summary(heading = Some("Welsh heading"), lead = Some("Welsh lead"), hint = Some("Welsh hint")))
          ))
          data.journeySetupDetails.sicCodes mustBe Seq("12345", "67890")
          await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 1
        }
      }
    }

    "return a Bad Request" when {
      "there was a problem validating the input json" in new Setup {
        setupSimpleAuthMocks()
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0

        assertFutureResponse(buildClient(initialiseJourneyUrl).withHttpHeaders(
          HeaderNames.COOKIE -> CookieBaker.getSessionCookie(),
          "X-Session-Id" -> CookieBaker.defaultSessionId,
          HeaderNames.AUTHORIZATION -> "test"
        ).post(Json.parse("""{"abc" : "xyz"}"""))) {
          res =>
            res.status mustBe BAD_REQUEST
            await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0
        }
      }

      "no session id could be found in request" in new Setup {
        setupSimpleAuthMocks()
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0

        assertFutureResponse(buildClient(initialiseJourneyUrl)
          .withHttpHeaders(HeaderNames.AUTHORIZATION -> "test")
          .post(Json.obj())) { res =>
            res.status mustBe BAD_REQUEST
            await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0
          }
      }
    }
  }
  "After initialising journey and user hits search /search-standard-industry-classification-codes" should {
    "return a 200 and user can post on page to search for results which also creates an entry in sicStore repo no exceptions occur" in new Setup {
      val sessionIdFullFlow = CookieBaker.getSessionCookie(Some("stubbed-123"))
      setupSimpleAuthMocks()
      await(journeyRepo.collection.countDocuments().toFuture()) mustBe 0
      await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0

      stubGet("/industry-classification-lookup/lookup/", 200, Some("{}"))

      assertFutureResponse(buildClient(initialiseJourneyUrl).withHttpHeaders(
        HeaderNames.COOKIE -> sessionIdFullFlow,
        "X-Session-Id" -> "stubbed-123",
        HeaderNames.AUTHORIZATION -> "test"
      ).post(setupJson)) { res =>
        res.status mustBe OK
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 1
        await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0

      }
      val identifiers = await(journeyRepo.collection.find().toFuture()).head.identifiers

      val journeyDataFromInitialisation = await(journeyRepo.retrieveJourneyData(identifiers))

      assertFutureResponse(buildClient(s"/sic-search/${identifiers.journeyId}/search-standard-industry-classification-codes").withHeaders(HeaderNames.COOKIE -> sessionIdFullFlow).get()) { res =>
        res.status mustBe OK
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 1
        await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 0
      }
      stubGETICLSearchResults
      assertFutureResponse(buildClient(s"/sic-search/${identifiers.journeyId}/search-standard-industry-classification-codes?doSearch=true").withHeaders(HeaderNames.COOKIE -> sessionIdFullFlow, "Csrf-Token" -> "nocheck").post(Map("sicSearch" -> Seq("dairy")))) { res =>
        res.status mustBe 303
        await(journeyRepo.collection.countDocuments().toFuture()) mustBe 1
        await(sicStoreRepo.collection.countDocuments().toFuture()) mustBe 1
      }

    }
  }

  "/internal/journeyID/fetch-results" must {
    "return an OK" when {
      "the journey exists and there are selected sic codes" in new Setup {
        setupSimpleAuthMocks()

        val sessionIdValue = "test-session-id"
        val sessionId: String = CookieBaker.getSessionCookie(sessionID = Some(sessionIdValue))

        val sicCodeChoices = List(SicCodeChoice(SicCode("12345", "test description", "test description"), Nil, Nil))
        val sicStore: SicStore = SicStore(journeyId, None, Some(sicCodeChoices))
        insertSicStore(sicStore)

        val journey: JourneyData = JourneyData(Identifiers(journeyId, sessionIdValue), "redirect-url", JourneySetup(queryBooster = Some(true)), Instant.now())
        insertJourney(journey)

        assertFutureResponse(buildClient(fetchResultsUrl).withHttpHeaders(
          HeaderNames.COOKIE -> sessionId,
          "X-Session-Id" -> sessionIdValue,
          HeaderNames.AUTHORIZATION -> "test"
        ).get()) { res =>
          res.status mustBe OK
          (res.json \ "sicCodes").as[List[SicCodeChoice]] mustBe sicCodeChoices
        }
      }
    }
    "return a 500" when {
      "there is no journey setup for the session" in new Setup {
        setupSimpleAuthMocks()

        val sessionIdValue = "test-session-id"
        val sessionId: String = CookieBaker.getSessionCookie(sessionID = Some(sessionIdValue))

        assertFutureResponse(buildClient(fetchResultsUrl).withHttpHeaders(
          HeaderNames.COOKIE -> sessionId,
          "X-Session-Id" -> sessionIdValue,
          HeaderNames.AUTHORIZATION -> "test"
        ).get()) { res =>
          res.status mustBe INTERNAL_SERVER_ERROR
        }
      }
      "there is a journey setup but no sic codes have been selected" in new Setup {
        setupSimpleAuthMocks()

        val sessionIdValue = "test-session-id"
        val sessionId: String = CookieBaker.getSessionCookie(sessionID = Some(sessionIdValue))

        val journey: JourneyData = JourneyData(Identifiers(journeyId, sessionIdValue), "redirect-url", JourneySetup(queryBooster = Some(true)), Instant.now())
        insertJourney(journey)

        assertFutureResponse(buildClient(fetchResultsUrl).withHttpHeaders(
          HeaderNames.COOKIE -> sessionId,
          "X-Session-Id" -> sessionIdValue,
          HeaderNames.AUTHORIZATION -> "test"
        ).get()) { res =>
          res.status mustBe NOT_FOUND
        }
      }
    }
  }
}

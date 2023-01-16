/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import models.setup.messages.{CustomMessages, Summary}
import models.setup.{JourneyData, JourneySetup}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

import java.util.UUID
import scala.util.Try

class JourneyDataSpec extends PlaySpec {

  def isValidUUID(uuid: String): Boolean = Try(UUID.fromString(uuid)).isSuccess

  val sessionId = "testSessionId"

  "initialRequestReads" should {
    "construct a JourneyData case class" when {
      "only given a redirectUrl and empty journeySetupDetails" in {
        val partialJson = Json.obj(
          "redirectUrl" -> "/test/uri"
        )

        val result = Json.fromJson(partialJson)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl mustBe "/test/uri"
        result.journeySetupDetails.customMessages mustBe None
        result.journeySetupDetails.dataSet mustBe JourneyData.ONS
        result.journeySetupDetails.queryParser mustBe None
        result.journeySetupDetails.queryBooster mustBe None
        result.journeySetupDetails.amountOfResults mustBe 50
        result.journeySetupDetails.sicCodes mustBe Seq.empty[String]
      }

      "given a redirect url and journeySetupDetails with custom messages" in {
        val partialJsonWithMessages = Json.obj(
          "redirectUrl" -> "/test/uri",
          "journeySetupDetails" -> Json.obj(
            "customMessages" -> Json.obj(
              "summary" -> Json.obj(
                "heading" -> "testMessage",
                "lead" -> "testMessage"
              )
            )
          )
        )

        val result = Json.fromJson(partialJsonWithMessages)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl mustBe "/test/uri"
        result.journeySetupDetails.customMessages mustBe Some(CustomMessages(
          summary = Some(Summary(heading = Some("testMessage"), lead = Some("testMessage"), hint = None)),
          summaryCy = None
        ))
      }

      "given a redirect url and journeySetupDetails with queryBooster" in {
        val partialJsonWithQueryBooster = Json.obj(
          "redirectUrl" -> "/test/uri",
          "journeySetupDetails" -> Json.obj(
            "queryBooster" -> true
          )
        )

        val result = Json.fromJson(partialJsonWithQueryBooster)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl mustBe "/test/uri"
        result.journeySetupDetails.queryBooster mustBe Some(true)
      }

      "given a redirect url and journeySetupDetails with amountOfResults set to 200" in {
        val partialJsonWithAmountOfResults = Json.obj(
          "redirectUrl" -> "/test/uri",
          "journeySetupDetails" -> Json.obj(
            "amountOfResults" -> 200
          )
        )

        val result = Json.fromJson(partialJsonWithAmountOfResults)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl mustBe "/test/uri"
        result.journeySetupDetails.amountOfResults mustBe 200
      }

      "given a redirect url and journeySetupDetails with a list of sic codes" in {
        val partialJsonWithSicCodes = Json.obj(
          "redirectUrl" -> "/test/uri",
          "journeySetupDetails" -> Json.obj(
            "sicCodes" -> Json.arr("12345", "67890")
          )
        )

        val result = Json.fromJson(partialJsonWithSicCodes)(JourneyData.initialRequestReads(sessionId)).get

        assert(isValidUUID(result.identifiers.journeyId))
        result.identifiers.sessionId mustBe sessionId

        result.redirectUrl mustBe "/test/uri"
        result.journeySetupDetails.sicCodes mustBe Seq("12345", "67890")
      }
    }
  }
  "journeySetup mongoWrites" should {
    "produce valid json with minimum data" in {
      val journeySetup = JourneySetup()
      val expectedJson = Json.parse(
        """
          |{
          |  "dataSet" : "ons-supplement-sic5",
          |  "amountOfResults" : 50,
          |  "sicCodes":[]
          |}
        """.stripMargin)
      Json.toJson(journeySetup)(JourneySetup.mongoWrites) mustBe expectedJson
    }

    "produce valid json with full data" in {
      val journeySetup = JourneySetup(
        dataSet = "foo",
        queryParser = Some(true),
        queryBooster = Some(true),
        amountOfResults = 200,
        customMessages = Some(CustomMessages(
          summary = Some(Summary(heading = Some("heading text"), lead = Some("lead text"), hint = Some("hint text"))),
          summaryCy = None
        )),
        sicCodes = Seq("12345", "67890")
      )
      val expectedJson = Json.parse(
        """
          |{
          |  "dataSet" : "foo",
          |  "queryParser" : true,
          |  "queryBooster" : true,
          |  "amountOfResults" : 200,
          |  "customMessages" : {
          |    "summary": {
          |      "heading": "heading text",
          |      "lead": "lead text",
          |      "hint": "hint text"
          |    }
          |  },
          |  "sicCodes" : [
          |    "12345",
          |    "67890"
          |  ]
          |}
        """.stripMargin)
      Json.toJson(journeySetup)(JourneySetup.mongoWrites) mustBe expectedJson
    }
  }
}

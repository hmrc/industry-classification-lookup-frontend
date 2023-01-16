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

package services

import helpers.UnitTestSpec
import helpers.mocks.MockJourneyDataRepo
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class JourneySetupServiceSpec extends UnitTestSpec with MockJourneyDataRepo {

  val now = LocalDateTime.now

  class Setup {
    val testService = new JourneyService(
      journeyDataRepository = mockJourneyDataRepository,
      sicSearchService = mockSicSearchService
    )
  }

  val lang = "en"
  val identifier = Identifiers(
    journeyId = "testJourneyId",
    sessionId = "testSessionId"
  )

  val journeyData = JourneyData(identifier, "/test/uri", JourneySetup(), now)

  val testJourneyData = JourneyData(
    identifiers = identifier,
    redirectUrl = "/test/uri",
    journeySetupDetails = JourneySetup(),
    lastUpdated = now
  )

  "initialiseJourney" should {
    "return Json containing the start and fetch uri's" in new Setup {
      mockInitialiseJourney(testJourneyData)
      when(mockSicSearchService.lookupSicCodes(any(), any())(any(), any())).thenReturn(Future.successful(0))

      assertAndAwait(testService.initialiseJourney(testJourneyData, lang)) {
        _ mustBe Json.obj(
          "journeyStartUri" -> s"/sic-search/testJourneyId/start-journey",
          "fetchResultsUri" -> s"/internal/testJourneyId/fetch-results"
        )
      }
    }
  }

  "getRedirectUrl" should {
    "return a redirect url" in new Setup {
      when(mockJourneyDataRepository.retrieveJourneyData(any()))
        .thenReturn(Future.successful(journeyData))

      assertAndAwait(testService.getRedirectUrl(identifier)) {
        _ mustBe "/test/uri"
      }
    }
  }
  "updateJourneyWithJourneySetup" should {
    val journeySetup = JourneySetup("foo", queryParser = None, None, 5)
    "return updated Journey Setup" in new Setup {
      when(mockJourneyDataRepository.updateJourneySetup(any(), any())).thenReturn(Future.successful(journeySetup))
      await(testService.updateJourneyWithJourneySetup(journeyData.identifiers, journeySetup)) mustBe journeySetup
    }
    "return an Exception" in new Setup {
      when(mockJourneyDataRepository.updateJourneySetup(any(), any())).thenReturn(Future.failed(new Exception("foo bar wizz bang")))
      intercept[Exception](await(testService.updateJourneyWithJourneySetup(journeyData.identifiers, journeySetup)))
    }
  }
  "getJourney" should {
    "get journey data successfully" in new Setup {
      when(mockJourneyDataRepository.retrieveJourneyData(any())).thenReturn(Future.successful(journeyData))
      await(testService.getJourney(journeyData.identifiers)) mustBe journeyData
    }
    "return an Exception" in new Setup {
      when(mockJourneyDataRepository.retrieveJourneyData(any())).thenReturn(Future.failed(new Exception("foo bar wizz bang")))
      intercept[Exception](await(testService.getJourney(journeyData.identifiers)))
    }
  }
}

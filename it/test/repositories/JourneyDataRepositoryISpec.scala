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

package repositories

import com.mongodb.client.model.Updates.unset
import models.setup.messages.{CustomMessages, Summary}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class JourneyDataRepositoryISpec extends PlaySpec with BeforeAndAfterEach with GuiceOneServerPerSuite {

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  class Setup {

    val repository: JourneyDataRepository = app.injector.instanceOf[JourneyDataRepository]
    await(repository.collection.drop.toFuture())

    def count: Long = await(repository.collection.countDocuments().toFuture())

    def insert(journeyData: JourneyData): InsertOneResult = await(repository.collection.insertOne(journeyData).toFuture())

    def fetchAll: List[JourneyData] = await(repository.collection.find().toFuture().map(_.toList))
  }

  def dateHasAdvanced(futureTime: Instant): Assertion = assert(futureTime.isAfter(now))

  val now: Instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)

  val customMsgs: CustomMessages = CustomMessages(
    summary = Some(Summary(
      heading = Some("testMessage1"),
      lead = Some("testMessage2"),
      hint = Some("testHint")
    )),
    summaryCy = None
  )

  val journeyData: JourneyData = JourneyData(
    identifiers = Identifiers(
      journeyId = "testJourneyId",
      sessionId = "testSessionId"
    ),
    redirectUrl = "test/url",
    journeySetupDetails = JourneySetup(queryBooster = Some(true), customMessages = Some(customMsgs)),
    lastUpdated = now
  )

  "upsertJourney" should {
    val data = JourneyData(
      identifiers = Identifiers(
        journeyId = "testJourneyId",
        sessionId = "testSessionId"
      ),
      redirectUrl = "test/url",
      journeySetupDetails = JourneySetup(queryBooster = Some(true), customMessages = None),
      lastUpdated = now
    )
    "successfully insert JourneyData into collection" in new Setup {
      await(repository.upsertJourney(data)) mustBe data
      count mustBe 1
      fetchAll mustBe List(data)
    }
    "update a record if one exists with different data" in new Setup {
      val updatedModel: JourneyData = data.copy(redirectUrl = "updated")
      await(repository.upsertJourney(data)) mustBe data
      count mustBe 1
      await(repository.retrieveJourneyData(data.identifiers)) mustBe data
      await(repository.upsertJourney(updatedModel)) mustBe updatedModel
      count mustBe 1
      await(repository.retrieveJourneyData(data.identifiers)) mustBe updatedModel

    }
    "update a record if one exists with the same data" in new Setup {
      await(repository.upsertJourney(data)) mustBe data
      count mustBe 1
      await(repository.retrieveJourneyData(data.identifiers)) mustBe data
      await(repository.upsertJourney(data)) mustBe data
      count mustBe 1
      await(repository.retrieveJourneyData(data.identifiers)) mustBe data
    }
  }
  "updateJourneySetup" should {
    "updateJourneySetup model within JourneyData Model successfully" in new Setup {
      val updatedJourneySetup: JourneySetup = JourneySetup("foo", Some(true), None, 10)
      await(repository.upsertJourney(journeyData)) mustBe journeyData
      count mustBe 1
      await(repository.updateJourneySetup(journeyData.identifiers, updatedJourneySetup)) mustBe updatedJourneySetup
      count mustBe 1
      await(repository.retrieveJourneyData(journeyData.identifiers)) mustBe journeyData.copy(journeySetupDetails = updatedJourneySetup)
    }
    "fail to update journeySetup and throw exception if no document exists" in new Setup {
      val validJourneySetup: JourneySetup = JourneySetup("foo", Some(true), None, 10)
      count mustBe 0
      intercept[Exception](await(repository.updateJourneySetup(journeyData.identifiers, validJourneySetup)))
      count mustBe 0
    }
  }

  "retrieveJourneyData" should {
    "successfully return a JourneyData" in new Setup {
      insert(journeyData)
      await(repository.retrieveJourneyData(journeyData.identifiers)).journeySetupDetails mustBe JourneySetup(queryBooster = Some(true), customMessages = Some(customMsgs))
    }
    "throw a RuntimeException when the journey does not exist in the repo" in new Setup {
      intercept[RuntimeException](await(repository.retrieveJourneyData(journeyData.identifiers)))
    }
    "throw a JsResultException when a journey exists but journey data isn't defined" in new Setup {
      case class JID(identifiers: Identifiers)

      implicit val writes: OWrites[JID] = Json.writes[JID]

      await(repository.collection.insertOne(journeyData).toFuture())
      await(repository.collection.updateOne(
        filter = Filters.equal("identifiers.journeyId", journeyData.identifiers.journeyId),
        update = unset("journeySetupDetails")).toFuture()
      )

      a[RuntimeException] mustBe thrownBy(await(repository.retrieveJourneyData(Identifiers(journeyId = "testJourneyId", sessionId = "testSessionId"))))
    }
  }

  "renewJourney" must {
    "update the lastUpdated value in the document to the current time" in new Setup {
      val beforeDateTime: Instant = Instant.now().minusSeconds(5)
      val afterDateTime: Instant = Instant.now().plusSeconds(5)

      insert(journeyData.copy(lastUpdated = Instant.parse("2000-01-01T12:00:00Z")))

      await(repository.renewJourney(journeyData.identifiers) {})

      fetchAll.head.lastUpdated isAfter beforeDateTime mustBe true
      fetchAll.head.lastUpdated isBefore afterDateTime mustBe true
    }
  }
}

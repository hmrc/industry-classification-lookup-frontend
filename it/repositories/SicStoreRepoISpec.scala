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

package repositories

import models._
import models.setup.{JourneyData, JourneySetup}
import org.mongodb.scala.result.InsertOneResult
import org.scalatest.concurrent.Eventually
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext.Implicits.global

class SicStoreRepoISpec extends PlaySpec with GuiceOneServerPerSuite with Eventually {

  class Setup {
    val repository: SicStoreRepository = app.injector.instanceOf[SicStoreRepository]

    await(repository.collection.drop.toFuture())

    def count: Long = await(repository.collection.countDocuments().toFuture())

    def insert(sicStore: SicStore): InsertOneResult = await(repository.collection.insertOne(sicStore).toFuture())

    def fetchAll: List[SicStore] = await(repository.collection.find.toFuture().map(_.toList))
  }

  val dateTime = LocalDateTime.parse("2017-06-15T10:06:28.434Z", DateTimeFormatter.ISO_DATE_TIME)

  val sessionId = "session-id-12345"
  val journeyId = "testJourneyId"
  val journey: String = JourneyData.QUERY_BUILDER
  val dataSet: String = JourneyData.ONS

  val sicCodeCode = "12345"
  val sicCode = SicCode(sicCodeCode, "Test sic code description", "Test sic code description")
  val sicCodeGroup = SicCodeChoice(sicCode, Nil, Nil)
  val sicCode2 = SicCode("87654", "Another test sic code description", "Another test sic code description")
  val sicCodeGroup2 = SicCodeChoice(sicCode2, Nil, Nil)

  val searchResults = SearchResults("testQuery", 1, List(sicCode), List(Sector("A", "Example", "Cy business sector", 1)))
  val searchResults2 = SearchResults("testQuery", 1, List(sicCode2), List(Sector("B", "Alternative", "Cy business sector", 1)))

  def generateSicStoreWithIndexes(indexes: List[String]) =
    SicStore(sessionId, Some(searchResults), Some(List(sicCodeGroup.copy(indexes = indexes, indexesCy = indexes))), dateTime)

  val sicStoreNoChoices = SicStore(journeyId, Some(searchResults), None, dateTime)
  val sicStore1Choice = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup)), dateTime)
  val sicStore2Choices = SicStore(journeyId, Some(searchResults2), Some(List(sicCodeGroup, sicCodeGroup2)), dateTime)
  val journeySetup = JourneySetup(dataSet, Some(false), Some(true), 50, None)

  "retrieveSicStore" should {
    "return a sic store when it is present" in new Setup {
      await(repository.collection.insertOne(sicStoreNoChoices).toFuture())
      await(repository.retrieveSicStore(journeyId)) mustBe Some(sicStoreNoChoices)
    }
    "return nothing when the reg id is not present" in new Setup {
      await(repository.retrieveSicStore(journeyId)) mustBe None
    }
  }

  "updateSearchResults" should {
    "insert a new document when one does not exist" in new Setup {
      count mustBe 0
      val updateSuccess: Boolean = await(repository.upsertSearchResults(journeyId, searchResults.copy(currentSector = Some(Sector("A", "Fake Sector", "Cy business sector", 1)))))

      updateSuccess mustBe true
      count mustBe 1
    }
    "update a document with the new sic code if the document already exists for a given session id" in new Setup {
      val otherSearchResults = SearchResults(
        "other query", 1,
        List(SicCode("87654", "Another test sic code description", "Another test sic code description")),
        List(Sector("A", "Fake", "Cy business sector", 1))
      )

      insert(sicStoreNoChoices)

      count mustBe 1

      val updateSuccess: Boolean = await(repository.upsertSearchResults(journeyId, otherSearchResults))

      updateSuccess mustBe true
      count mustBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.searchResults mustBe Some(otherSearchResults)
      fetchedDocument.lastUpdated isAfter sicStoreNoChoices.lastUpdated mustBe true
    }
  }

  "insertChoice" should {
    "insert new sic codes into empty sic store" in new Setup {
      count mustBe 0

      val sicCode2 = SicCodeChoice(SicCode("67891", "some description", "some description"))
      val insertSuccess: Boolean = await(repository.insertChoices(journeyId, List(sicCodeGroup, sicCode2)))

      insertSuccess mustBe true
      count mustBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices mustBe Some(List(sicCodeGroup, sicCode2))
    }
    "insert a new sic code into a choices list with no other choices" in new Setup {
      insert(sicStoreNoChoices)
      count mustBe 1

      val insertSuccess: Boolean = await(repository.insertChoices(journeyId, List(sicCodeGroup)))

      insertSuccess mustBe true
      count mustBe 1

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices mustBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated mustBe true
    }
    "insert a new sic code choice into a choices list with another choice already there" in new Setup {
      val sicCodeToAdd = SicCode("67891", "some description", "some description")

      val searchResults = SearchResults("testQuery", 1, List(sicCodeToAdd), List(Sector("A", "Fake", "Cy business sector", 1)))
      val sicStoreWithExistingChoice = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup)), dateTime)

      insert(sicStoreWithExistingChoice)

      await(repository.insertChoices(journeyId, List(SicCodeChoice(sicCodeToAdd))))

      val fetchedDocument: SicStore = fetchAll.head
      val sicStoreWith2Choices = SicStore(journeyId, Some(searchResults), Some(List(sicCodeGroup, SicCodeChoice(sicCodeToAdd))), dateTime)

      fetchedDocument.choices mustBe sicStoreWith2Choices.choices
      fetchedDocument.lastUpdated isAfter sicStoreWith2Choices.lastUpdated mustBe true
    }
    "inserting the same choice twice will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoices(journeyId, List(sicCodeGroup)))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices mustBe sicStore1Choice.choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated mustBe true
    }
    "inserting the same sicCode with different indexes will not duplicate it in the documents choices" in new Setup {
      insert(sicStore1Choice)

      await(repository.insertChoices(journeyId, List(SicCodeChoice(sicCode, List("some description"), List("some description")))))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices mustBe generateSicStoreWithIndexes(List("some description")).choices
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated mustBe true
    }
    "inserting the same sicCode with different indexes will update the correct choice + add a new choice" in new Setup {
      val sicCode3 = SicCode("67891", "some other description", "some other description")
      val sicCodeGroup3 = SicCodeChoice(sicCode3, List("some index 1"), List("some index 1"))
      val sicCode4 = SicCode("11122", "test desc", "test desc")
      val sicCodeGroup4 = SicCodeChoice(sicCode4, List("index whatever test"), List("index whatever test"))
      val sicStore3Choices = SicStore(journeyId, Some(searchResults2), Some(List(sicCodeGroup, sicCodeGroup2, sicCodeGroup3)), dateTime)
      insert(sicStore3Choices)

      val expected = Some(List(sicCodeGroup.copy(indexes = List("some description")), sicCodeGroup2, sicCodeGroup3.copy(indexes = List("some index 1", "new test other desc"), indexesCy = List("some index 1", "new test other desc")), sicCodeGroup4))

      await(repository.insertChoices(journeyId, List(sicCodeGroup.copy(indexes = List("some description")), SicCodeChoice(sicCode3, List("new test other desc"), List("new test other desc")), sicCodeGroup4)))

      val fetchedDocument: SicStore = fetchAll.head

      fetchedDocument.choices mustBe expected
      fetchedDocument.lastUpdated isAfter sicStore3Choices.lastUpdated mustBe true
    }
  }

  "removeChoice" should {
    "remove a choice from the list of choices held in the document" in new Setup {
      await(repository.collection.insertOne(sicStore2Choices).toFuture())
      await(repository.removeChoice(journeyId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(journeyId)).get

      fetchedDocument.choices mustBe Some(List(sicCodeGroup2))
      fetchedDocument.lastUpdated isAfter sicStore2Choices.lastUpdated mustBe true
    }
    "remove a choice from the choice list leaving no choices" in new Setup {
      await(repository.collection.insertOne(sicStore1Choice).toFuture())
      await(repository.removeChoice(journeyId, sicCode.sicCode))

      val fetchedDocument: SicStore = await(repository.retrieveSicStore(journeyId)).get

      fetchedDocument.choices mustBe Some(List())
      fetchedDocument.lastUpdated isAfter sicStore1Choice.lastUpdated mustBe true
    }
    "return None if unable to remove the choice from the list" in new Setup {
      await(repository.removeChoice(journeyId, sicCode.sicCode))
      await(repository.retrieveSicStore(journeyId)) mustBe None
    }
  }

}

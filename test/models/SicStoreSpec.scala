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

package models

import helpers.UnitTestSpec
import org.joda.time.DateTime
import play.api.libs.json.{JsSuccess, JsValue, Json}
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

class SicStoreSpec extends UnitTestSpec {

  val query = "testQuery"
  val journey: String = Journey.QUERY_BUILDER
  val dataSet: String = Journey.ONS
  val dateTime: DateTime = DateTime.parse("2017-06-15T10:06:28.434Z")
  val now: JsValue = Json.toJson(dateTime)(ReactiveMongoFormats.dateTimeWrite)

  val sicStoreWithChoicesJson : JsValue = Json.parse(
    s"""
      |{
      |  "sessionId" : "12345",
      |  "journey" : "$journey",
      |  "dataSet" : "$dataSet",
      |  "search" : {
      |    "query":"$query",
      |    "numFound":1,
      |    "results":[
      |      {"code" : "19283", "desc" : "Search Sic Code Result Description"}
      |    ],
      |    "sectors":[
      |      {"code" : "A", "name" : "Clearly fake business sector", "count": 22}
      |    ]
      |  },
      |  "choices" : [
      |    {"code" : "57384", "desc" : "Sic Code Test Description 1", "indexes": ["someIndex 1"]},
      |    {"code" : "11920", "desc" : "Sic Code Test Description 2", "indexes": ["someIndex 2"]},
      |    {"code" : "12994", "desc" : "Sic Code Test Description 3", "indexes": ["someIndex 3"]},
      |    {"code" : "39387", "desc" : "Sic Code Test Description 4", "indexes": []}
      |  ],
      |  "lastUpdated" : $now
      |}
    """.stripMargin
  )

  val sicStoreNoChoicesJson : JsValue = Json.parse(
    s"""
      |{
      |  "sessionId" : "12345",
      |  "journey" : "$journey",
      |  "dataSet" : "$dataSet",
      |  "search" : {
      |     "query":"$query",
      |     "numFound":1,
      |     "results":[
      |       {"code" : "19283", "desc" : "Search Sic Code Result Description"}
      |     ],
      |     "sectors":[
      |       {"code" : "A", "name" : "Clearly fake business sector", "count": 22}
      |     ]
      |  },
      |  "lastUpdated" : $now
      |}
    """.stripMargin
  )

  val sicStoreWithChoices = SicStore(
    "12345",
    journey,
    dataSet,
    Some(SearchResults(
      query,
      1,
      List(SicCode("19283", "Search Sic Code Result Description")),
      List(Sector("A", "Clearly fake business sector", 22))
    )),
    Some(List(
      SicCodeChoice(SicCode("57384", "Sic Code Test Description 1"), List("someIndex 1")),
      SicCodeChoice(SicCode("11920", "Sic Code Test Description 2"), List("someIndex 2")),
      SicCodeChoice(SicCode("12994", "Sic Code Test Description 3"), List("someIndex 3")),
      SicCodeChoice(SicCode("39387", "Sic Code Test Description 4"), List())
    )),
    dateTime
  )

  val sicStoreNoChoices = SicStore(
    "12345",
    journey,
    dataSet,
    Some(SearchResults(
      query,
      1,
      List(SicCode("19283", "Search Sic Code Result Description")),
      List(Sector("A", "Clearly fake business sector", 22))
    )),
    None,
    dateTime
  )

  "SicStore" should {

    "be able to be parsed into a json structure with choices" in {
      Json.toJson(sicStoreWithChoices)(SicStore.format) mustBe sicStoreWithChoicesJson
    }

    "be able to be parsed into a json structure without choices" in {
      Json.toJson(sicStoreNoChoices)(SicStore.format) mustBe sicStoreNoChoicesJson
    }

    "be able to be parsed from json structure with choices" in {
      Json.fromJson(sicStoreWithChoicesJson)(SicStore.format) mustBe JsSuccess(sicStoreWithChoices)
    }

    "be able to be parsed from json structure without choices" in {
      Json.fromJson(sicStoreNoChoicesJson)(SicStore.format) mustBe JsSuccess(sicStoreNoChoices)
    }
  }
}

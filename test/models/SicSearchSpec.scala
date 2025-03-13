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

import helpers.UnitTestSpec
import play.api.libs.json.{JsValue, Json}

class SicSearchSpec extends UnitTestSpec {

  val testSicSearchModel: SicSearch = SicSearch("12345")

  val testSicSearchJson: JsValue = Json.parse(
    """
      |{
      |  "sicSearch":"12345"
      |}
    """.stripMargin
  )

  "Sic Search Model" should {

    "read from json with data" in {
      Json.fromJson(testSicSearchJson)(SicSearch.format).get mustBe testSicSearchModel
    }

    "write to json with data" in {
      Json.toJson(testSicSearchModel)(SicSearch.format) mustBe testSicSearchJson
    }
  }
}

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

package forms

import forms.chooseactivity.ChooseMultipleActivitiesForm
import models.{SearchResults, SicCode}
import org.scalatestplus.play.PlaySpec
import play.api.data.FormBinding.Implicits.formBinding
import play.api.data.FormError
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest

class ChooseMultipleActivityFormSpec extends PlaySpec {

  val inputMap: Map[String, String] = Map(
    "code[0]" -> "testCode0-testDescription0",
    "code[1]" -> "testCode1-testDescription1",
    "code[2]" -> "testCode2-testDescription2",
    "code[3]" -> "testCode3-testDescription3",
    "code[4]" -> "testCode4-testDescription4"
  )

  implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest().withFormUrlEncodedBody(inputMap.toSeq: _*)

  val inputMap2: Map[String, String] = Map(
    "code[0]" -> "testCode0-testDescription0",
    "code[1]" -> "testCode1-testDescription1",
    "code[2]" -> "",
    "code[3]" -> "testCode3-testDescription3",
    "code[4]" -> "testCode4-testDescription4"
  )

  val expectedSicCodeList = List(
    SicCode("testCode0", "testDescription0", "testDescription0"),
    SicCode("testCode1", "testDescription1", "testDescription1"),
    SicCode("testCode2", "testDescription2", "testDescription2"),
    SicCode("testCode3", "testDescription3", "testDescription3"),
    SicCode("testCode4", "testDescription4", "testDescription4")
  )

  val searchResults = SearchResults("test-query", 1, expectedSicCodeList, List())

  "SicSearchMultipleForm" should {
    "bind successfully" when {
      "binding from a request" in {
        ChooseMultipleActivitiesForm.form(Some(searchResults)).bindFromRequest().get mustBe expectedSicCodeList
      }

      "binding from a map and transform data" in {
        ChooseMultipleActivitiesForm.form(Some(searchResults)).bind(inputMap).get mustBe expectedSicCodeList
      }

      "binding from a map" in {
        ChooseMultipleActivitiesForm.form(Some(searchResults)).bind(inputMap).get mustBe expectedSicCodeList
      }

      "binding from a map with gaps" in {
        ChooseMultipleActivitiesForm.form(Some(searchResults)).bind(inputMap2).get mustBe List(
          SicCode("testCode0", "testDescription0", "testDescription0"),
          SicCode("testCode1", "testDescription1", "testDescription1"),
          SicCode("testCode3", "testDescription3", "testDescription3"),
          SicCode("testCode4", "testDescription4", "testDescription4")
        )
      }
    }

    "return an empty list when unbinding from sic code" in {
      ChooseMultipleActivitiesForm.form(None).mapping.unbind(expectedSicCodeList) mustBe Map()
    }

    "bind unsuccessfully" when {
      val emptyErrors = List(
        FormError("code", List("errors.invalid.sic.noSelection"))
      )

      "nothing is the request" in {
        ChooseMultipleActivitiesForm.form(None).bindFromRequest()(FakeRequest(), formBinding).errors mustBe emptyErrors
      }

      "the map is empty" in {
        ChooseMultipleActivitiesForm.form(None).bind(Map.empty[String, String]).errors mustBe emptyErrors
      }
    }
  }
}

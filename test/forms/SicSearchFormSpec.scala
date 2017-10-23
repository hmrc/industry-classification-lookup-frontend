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

package models

import forms.sicsearch.SicSearchForm
import uk.gov.hmrc.play.test.UnitSpec

class SicSearchFormSpec extends UnitSpec {
  val testForm = SicSearchForm.form

  "Binding SicSearchFormSpec to a model" should {
    "Bind successfully with full data" in {
      val data = Map(
        "sicSearch" -> "12345678"
      )
      val model = SicSearch(
        sicSearch = "12345678"
      )
      val boundModel = testForm.bind(data).fold(
        errors => errors,
        success => success
      )

      boundModel shouldBe model
    }
  }

}

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

import forms.sicsearch.SicSearchForm
import helpers.UnitTestSpec
import models.SicSearch
import play.api.data.{Form, FormError}

class SicSearchFormSpec extends UnitTestSpec {
  val testForm: Form[SicSearch] = SicSearchForm.form

  "Binding SicSearchFormSpec to a model" should {
    "Bind successfully with full data" in {
      val data      = Map("sicSearch" -> "12345")
      val model     = SicSearch(sicSearch = "12345")
      val boundForm = testForm.bind(data).fold(errors => errors, success => success)

      boundForm mustBe model
    }

    "Provide the correct error when nothing was entered" in {
      val data      = Map("sicSearch" -> "")
      val model     = Seq(FormError("sicSearch", "errors.invalid.sic.noEntry"))
      val boundForm = testForm.bind(data).fold(errors => errors, success => testForm.fill(success))

      boundForm.errors mustBe model
      boundForm.data mustBe data
    }

    "Provide the correct error when the search entered is more than 100 characters" in {
      val data = Map(
        "sicSearch" -> "qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio qwertyuio q"
      )
      val model     = Seq(FormError("sicSearch", "errors.invalid.sic.noEntry"))
      val boundForm = testForm.bind(data).fold(errors => errors, success => testForm.fill(success))

      boundForm.errors mustBe model
      boundForm.data mustBe data
    }
  }
}

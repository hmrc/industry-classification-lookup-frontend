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

import helpers.UnitTestSpec
import play.api.data.{Form, FormError}

class RemoveSicCodeFormSpec extends UnitTestSpec {

  val testForm: Form[String] = RemoveSicCodeForm.form("description")

  "Binding BusinessActivityFormSpec to a model" should {
    "bind successfully with full data" in {

      val data = Map("removeCode" -> "yes")
      val boundForm = testForm.bind(data).fold(errors => errors, success => success)

      boundForm mustBe "yes"
    }

    "provide the correct error when nothing was selected" in {
      val data = Map("removeCode" -> "")
      val model = Seq(FormError("removeCode", "errors.invalid.sic.remove", Seq("description")))
      val boundForm = testForm.bind(data).fold(errors => errors, success => testForm.fill(success))

      boundForm.errors mustBe model
      boundForm.data mustBe data
    }
  }

}

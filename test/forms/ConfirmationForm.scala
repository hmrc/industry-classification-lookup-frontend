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

import forms.ConfirmationForm
import forms.chooseactivity.ChooseActivityForm
import play.api.data.FormError
import uk.gov.hmrc.play.test.UnitSpec

class ConfirmationForm extends UnitSpec {

  val testForm = ConfirmationForm.form

  "Binding BusinessActivityFormSpec to a model" should {
    "bind successfully with full data" in {

      val data = Map("addAnother" -> "yes")
      val model = Confirmation(addAnother = "yes")
      val boundForm = testForm.bind(data).fold(errors => errors, success => success)

      boundForm shouldBe model
    }

    "provide the correct error when nothing was selected" in {
      val data = Map("addAnother" -> "")
      val model = Seq(FormError("addAnother", "errors.invalid.sic.confirm"))
      val boundForm = testForm.bind(data).fold(errors => errors, success => testForm.fill(success))

      boundForm.errors shouldBe model
      boundForm.data shouldBe data
    }
  }

}
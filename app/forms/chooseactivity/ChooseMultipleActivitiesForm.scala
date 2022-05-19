/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.chooseactivity

import models.SicCode
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}

object ChooseMultipleActivitiesForm {
  val toSicValuePair: (String) => SicCode = sicVal => {
    val sicValueSplit = sicVal.split("-",2)
    SicCode(sicValueSplit.head, sicValueSplit.last)
  }

  def validateList: Mapping[List[SicCode]] = {
    val textConstraint: Constraint[List[String]] = Constraint {
      case s if s.isEmpty => Invalid(ValidationError("errors.invalid.sic.noSelection"))
      case _              => Valid
    }
    list(text)
      .verifying(textConstraint)
      .transform[List[SicCode]](x => x.filterNot(_.isEmpty).map(toSicValuePair), _ => List.empty[String])
  }

  val form = Form(
    single(
      "code" -> validateList
    )
  )
}

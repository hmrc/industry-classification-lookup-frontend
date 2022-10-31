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

import models.{SearchResults, SicCode}
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}

object ChooseMultipleActivitiesForm {
  val toSicValuePair: (String, Option[SearchResults]) => Option[SicCode] = (sicVal, maybeSearchResult) => {
    val Array(code, descr) = sicVal.split("-",2)
    maybeSearchResult match {
      case Some(searchResult) =>
        searchResult.results.find(sc => {
          sc.sicCode == code && (sc.descriptionCy == descr || sc.description == descr)
        })
      case None => None
    }
  }

  def validateList(searchResults: Option[SearchResults] = None): Mapping[List[SicCode]] = {
    val textConstraint: Constraint[List[String]] = Constraint {
      case s if s.isEmpty => Invalid(ValidationError("errors.invalid.sic.noSelection"))
      case _              => Valid
    }
    list(text)
      .verifying(textConstraint)
      .transform[List[SicCode]](x => x.filterNot(_.isEmpty).flatMap(value => toSicValuePair(value, searchResults)), _ => List.empty[String])
  }

  val form: Option[SearchResults] => Form[List[SicCode]] = results => Form(
    single("code" -> validateList(results))
  )
}

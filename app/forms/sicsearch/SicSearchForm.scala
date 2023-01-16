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

package forms.sicsearch

import models.SicSearch
import play.api.data.Forms.mapping
import play.api.data.format.Formatter
import play.api.data.{Form, FormError, Forms, Mapping}

object SicSearchForm {

  implicit def sicSearchFormatter: Formatter[String] = new Formatter[String] {

    def validate(entry: String): Either[Seq[FormError], String] = {
      entry match {
        case ""  => Left(Seq(FormError("sicSearch", "errors.invalid.sic.noEntry")))
        case ss if ss.length > 100 => Left(Seq(FormError("sicSearch", "errors.invalid.sic.noEntry")))
        case ss  => Right(ss)
      }
    }

    override def bind(key: String, data: Map[String, String]) = {
      validate(data.getOrElse(key, ""))
    }

    override def unbind(key: String, value: String) = Map(key -> value)
  }

  def sicSearchField: Mapping[String] = Forms.of[String](sicSearchFormatter)

  val form = Form(
    mapping(
      "sicSearch" -> sicSearchField
    )(SicSearch.apply)(SicSearch.unapply)
  )
}

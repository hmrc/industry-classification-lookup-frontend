/*
 * Copyright 2024 HM Revenue & Customs
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

package helpers

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, Json}

trait ICLStub {
  val sicCodeLookupResult: JsObject = Json.obj(
    "numFound" -> 36,
    "nonFilteredFound" -> 36,
    "results" -> Json.arr(
      Json.obj("code" -> "01410003", "desc" -> "Dairy farming", "descCy" -> "Dairy farming"),
      Json.obj("code" -> "01420003", "desc" -> "Cattle farming", "descCy" -> "Cattle farming"),
      Json.obj("code" -> "03220009", "desc" -> "Cattle farming", "descCy" -> "Cattle farming"),
      Json.obj("code" -> "01490008", "desc" -> "Fur farming", "descCy" -> "Fur farming"),
      Json.obj("code" -> "01490026", "desc" -> "Snail farming", "descCy" -> "Snail farming")
    ),
    "sectors" -> Json.arr(
      Json.obj("code" -> "A", "name" -> "Agriculture, Forestry And Fishing", "nameCy" -> "Cy business sector", "count" -> 19),
      Json.obj("code" -> "C", "name" -> "Manufacturing", "nameCy" -> "Cy business sector", "count" -> 9),
      Json.obj("code" -> "G", "name" -> "Wholesale And Retail Trade; Repair Of Motor Vehicles And Motorcycles", "nameCy" -> "Cy business sector", "count" -> 7),
      Json.obj("code" -> "N", "name" -> "Administrative And Support Service Activities", "nameCy" -> "Cy business sector", "count" -> 1)
    )
  )
  def stubGETICLSearchResults: StubMapping = {
  stubFor(get(urlMatching("/industry-classification-lookup/search?.*"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(sicCodeLookupResult.toString().stripMargin)
    ))
}
}
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

package models.setup

import models.setup.messages.CustomMessages
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.LocalDateTime
import java.util.UUID

case class JourneyData(identifiers: Identifiers,
                       redirectUrl: String,
                       journeySetupDetails: JourneySetup,
                       lastUpdated: LocalDateTime)

case class Identifiers(journeyId: String, sessionId: String)

object Identifiers {
  implicit val format: Format[Identifiers] = Json.format[Identifiers]
}

case class JourneySetup(dataSet: String = JourneyData.ONS,
                        queryParser: Option[Boolean] = None,
                        queryBooster: Option[Boolean] = None,
                        amountOfResults: Int = JourneySetup.DEFAULT_AMOUNT_OF_RESULTS,
                        customMessages: Option[CustomMessages] = None,
                        sicCodes: Seq[String] = Seq.empty[String])
object JourneySetup {
  val DEFAULT_AMOUNT_OF_RESULTS = 50

  private def journeyDetailsSetupApply(queryBooster: Option[Boolean], amountOfResults: Int, customMessages: Option[CustomMessages], sicCodes: Seq[String]): JourneySetup =
    JourneySetup(JourneyData.ONS, None, queryBooster, amountOfResults, customMessages, sicCodes)

  val initialRequestReads: Reads[JourneySetup] = (
    (__ \ "queryBooster").readNullable[Boolean] and
    ((__ \ "amountOfResults").read[Int] or Reads.pure(DEFAULT_AMOUNT_OF_RESULTS)) and
    (__ \ "customMessages").readNullable[CustomMessages] and
    ((__ \ "sicCodes").read[Seq[String]] or Reads.pure(Seq.empty[String]))
  )(JourneySetup.journeyDetailsSetupApply _)

  val mongoWrites: Writes[JourneySetup] = new Writes[JourneySetup] {
    override def writes(o: JourneySetup): JsValue = {
      Json.obj(
        "dataSet" -> o.dataSet,
        "amountOfResults" -> o.amountOfResults,
        "sicCodes" -> o.sicCodes
      ) ++
      o.queryParser.fold(Json.obj())(v => Json.obj("queryParser" -> v)) ++
      o.queryBooster.fold(Json.obj())(v => Json.obj("queryBooster" -> v)) ++
      o.customMessages.fold(Json.obj())(v => Json.obj("customMessages" -> v))
    }
  }
}

object JourneyData extends MongoJavatimeFormats {
  //Journeys
  val QUERY_BUILDER = "query-builder"
  val QUERY_PARSER  = "query-parser"
  val QUERY_BOOSTER = "query-boost-first-term"
  val journeyNames = Seq(QUERY_PARSER, QUERY_BUILDER, QUERY_BOOSTER)
  //Data sets
  val GDS        = "gds-register-sic5"
  val ONS        = "ons-supplement-sic5"
  val dataSets = Seq(GDS, ONS)



  implicit val journeySetupFormat: Format[JourneySetup] = Json.format[JourneySetup]

  implicit val format: OFormat[JourneyData] = (
    (__ \ "identifiers").format[Identifiers] and
    (__ \ "redirectUrl").format[String] and
    (__ \ "journeySetupDetails").format[JourneySetup] and
    (__ \ "lastUpdated").format[LocalDateTime](Implicits.jatLocalDateTimeFormat)
  )(JourneyData.apply, unlift(JourneyData.unapply))

  def initialRequestReads(sessionId: String): Reads[JourneyData] = (
    (__ \ "identifiers").read(Identifiers(UUID.randomUUID().toString, sessionId)) and
    (__ \ "redirectUrl").read[String] and
    ((__ \ "journeySetupDetails").read[JourneySetup](JourneySetup.initialRequestReads) or Reads.pure(JourneySetup())) and
    (__ \ "lastUpdated").read(LocalDateTime.now)
  )(JourneyData.apply _)
}
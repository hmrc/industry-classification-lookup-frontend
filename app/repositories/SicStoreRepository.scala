/*
 * Copyright 2021 HM Revenue & Customs
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

package repositories

import models.{SearchResults, SicCodeChoice, SicStore}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Configuration
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.{BSONDocument, BSONObjectID, BSONString}
import reactivemongo.play.json.BSONFormats
import reactivemongo.play.json.ImplicitBSONHandlers.BSONDocumentWrites
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicStoreRepository @Inject()(config: Configuration,
                                   mongo: ReactiveMongoComponent
                                  )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[SicStore, BSONObjectID](
    "sic-store",
    mongo.mongoConnector.db,
    SicStore.format
  ) with TTLIndexing[SicStore, BSONObjectID] {

  private[repositories] def sessionIdSelector(sessionId: String): BSONDocument = BSONDocument(
    "sessionId" -> BSONString(sessionId)
  )

  private def journeyIdSelector(journeyId: String) = BSONDocument("journeyId" -> journeyId)

  override val ttl: Long = config.get[Long]("mongodb.timeToLiveInSeconds")

  private[repositories] def now = DateTime.now(DateTimeZone.UTC)

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    super.ensureIndexes flatMap { l =>
      ensureTTLIndexes map {
        ttl => l ++ ttl
      }
    }
  }

  def retrieveSicStore(journeyId: String)(implicit ec: ExecutionContext): Future[Option[SicStore]] = {
    collection.find(journeyIdSelector(journeyId)).one[SicStore]
  }

  def upsertSearchResults(journeyId: String, searchResults: SearchResults)(implicit ec: ExecutionContext): Future[Boolean] = {
    val selector = journeyIdSelector(journeyId)

    val searchjson = Json.obj(
      "search" -> Json.toJson(searchResults).as[JsObject],
      "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite))

    val update = BSONDocument("$set" -> BSONFormats.readAsBSONValue(searchjson).get)
    collection.update(selector, update, upsert = true).map(_.ok)
  }

  private def getChoicesJsonObject(sicCodes: List[SicCodeChoice]): JsValue = JsArray(sicCodes.map {
    scc =>
      Json.obj(
        "code" -> scc.code,
        "desc" -> scc.desc,
        "indexes" -> scc.indexes
      )
  })

  private def getIndexesJsonObject(strings: List[String]): JsValue = JsArray(strings.map(Json.toJson[String]))

  def insertChoices(journeyId: String, sicCodes: List[SicCodeChoice])(implicit ec: ExecutionContext): Future[Boolean] = {
    def addChoices(journeyId: String, choices: List[SicCodeChoice]): Future[Boolean] = {
      if (choices.isEmpty) {
        Future.successful(true)
      } else {
        val selector = journeyIdSelector(journeyId)

        val set = Json.obj(
          "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
        )

        val add = BSONDocument(
          "$addToSet" -> BSONDocument("choices" -> BSONDocument("$each" -> BSONFormats.readAsBSONValue(getChoicesJsonObject(choices)).get)),
          "$set" -> BSONFormats.readAsBSONValue(set).get
        )

        collection.update(selector, add, upsert = true) map (_.ok)
      }
    }

    retrieveSicStore(journeyId) flatMap { res =>
      res.fold(addChoices(journeyId, sicCodes)) { store =>
        val (toUpdate, toAdd) = store.choices match {
          case Some(choices) =>
            val mapChoiceIndex = choices.map(_.code).zipWithIndex.toMap
            val parts = sicCodes partition (x => choices.exists(_.code == x.code))
            (parts._1.map(sic => (sic, mapChoiceIndex(sic.code))), parts._2)
          case None => (Nil, sicCodes)
        }

        val set = Json.obj(
          "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
        )

        val updateIndexes = toUpdate.foldLeft(Json.obj()) { (json, tuple) =>
          val (sicCodeChoice, index) = tuple
          val indexes = store.choices match {
            case Some(list) =>
              list.find(_.code == sicCodeChoice.code)
                .fold(sicCodeChoice.indexes)(choice => (choice.indexes ++ sicCodeChoice.indexes).distinct)
            case None => sicCodeChoice.indexes
          }
          val update = Json.obj(s"choices.$index.indexes" -> BSONDocument("$each" -> BSONFormats.readAsBSONValue(getIndexesJsonObject(indexes)).get))

          json ++ update
        }

        if (toUpdate.isEmpty) {
          addChoices(journeyId, toAdd)
        } else {
          val update = BSONDocument(
            "$addToSet" -> BSONFormats.readAsBSONValue(updateIndexes).get,
            "$set" -> BSONFormats.readAsBSONValue(set).get
          )
          collection.update(journeyIdSelector(journeyId), update, upsert = true) flatMap (_ => addChoices(journeyId, toAdd))
        }
      }
    }
  }

  def removeChoice(journeyId: String, sicCode: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    retrieveSicStore(journeyId) flatMap {
      case Some(_) =>
        val selector = journeyIdSelector(journeyId)

        val pull = Json.obj(
          "choices" -> Json.obj("code" -> sicCode)
        )

        val set = Json.obj(
          "lastUpdated" -> Json.toJson(now)(ReactiveMongoFormats.dateTimeWrite)
        )

        val update = BSONDocument(
          "$pull" -> BSONFormats.readAsBSONValue(pull).get,
          "$set" -> BSONFormats.readAsBSONValue(set).get
        )

        collection.update(selector, update).map(_.nModified == 1)
      case None => Future.successful(false)
    }
  }
}
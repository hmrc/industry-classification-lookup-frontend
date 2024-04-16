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

package repositories

import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Indexes.ascending
import com.mongodb.client.model.Updates.{currentDate, pull, set}
import config.Logging
import models.{SearchResults, SicCodeChoice, SicStore}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model
import org.mongodb.scala.model.Filters.{equal, exists}
import org.mongodb.scala.model.Updates.{addEachToSet, combine}
import org.mongodb.scala.model.{IndexOptions, UpdateOptions}
import play.api.Configuration
import play.api.mvc.Request
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicStoreRepository @Inject() (config: Configuration, mongo: MongoComponent)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[SicStore](
      mongoComponent = mongo,
      collectionName = "sic-store",
      domainFormat = SicStore.format,
      indexes = Seq(
        model.IndexModel(
          keys = ascending("lastUpdated"),
          indexOptions = IndexOptions()
            .name("lastUpdatedIndex")
            .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        )
      )
    )
    with MongoJavatimeFormats
    with Logging {

  collection.find[Document](or(equal("journeyId", null), exists("journeyId", exists = false)))
    .subscribe { found =>
      val id = found.getObjectId("_id")
      logger.info(s"[SicStore] Found record missing journeyId field: $id. Attempting to delete...")
      collection.deleteOne(equal("_id", id)).subscribe(delete =>
        if (delete.wasAcknowledged() && delete.getDeletedCount > 0) {
          logger.info(s"[SicStore] Deleted record $id")
        }
      )
    }

  private def journeyIdSelector(journeyId: String): Bson = equal("journeyId", journeyId)

  private[repositories] def now: Long = Instant.now().toEpochMilli

  def retrieveSicStore(journeyId: String): Future[Option[SicStore]] =
    collection.find[SicStore](journeyIdSelector(journeyId))
      .headOption()

  def upsertSearchResults(journeyId: String, searchResults: SearchResults): Future[Boolean] =
    collection.updateOne(
      filter = journeyIdSelector(journeyId),
      update = combine(currentDate("lastUpdated"), set("search", Codecs.toBson(searchResults))),
      options = UpdateOptions().upsert(true)
    )
      .toFuture()
      .map(_.wasAcknowledged)

  def insertChoices(journeyId: String, sicCodes: List[SicCodeChoice])(implicit request: Request[_]): Future[Boolean] = {
    def addChoices(journeyId: String, choices: List[SicCodeChoice]): Future[Boolean] = {
      infoLog(s"[SicStoreRepository][insertChoices] Adding new SIC code choices for journey ID $journeyId")
      if (choices.isEmpty) {
        Future.successful(true)
      } else {
        collection.updateOne(
          filter = journeyIdSelector(journeyId),
          update = combine(currentDate("lastUpdated"), addEachToSet("choices", choices.map(Codecs.toBson(_)): _*)),
          options = UpdateOptions().upsert(true)
        )
          .toFuture()
          .map(_.wasAcknowledged())
      }
    }

    retrieveSicStore(journeyId) flatMap { optSicStore =>
      optSicStore.fold(addChoices(journeyId, sicCodes)) { sicStore =>
        val (codesToUpdate, newCodes) = sicStore.choices match {
          case Some(choices) =>
            val indexedChoices = choices.map(_.code).zipWithIndex.toMap
            val parts          = sicCodes.partition(choice => choices.exists(_.code == choice.code))
            (parts._1.map(sic => (sic, indexedChoices(sic.code))), parts._2)
          case None => (Nil, sicCodes)
        }

        val updateIndexes = codesToUpdate.foldLeft(List[Bson]()) { (acc, tuple) =>
          val (sicCodeChoice, n) = tuple

          /* SIC codes can have several business activities associated with them. Each activity has a unique description,
          which are here referred to as "indexes". This combines the indexes when several selections exist for the same SIC code */
          def combineIndexes(indexes: SicCodeChoice => List[String]): List[String] =
            sicStore.choices match {
              case Some(list) =>
                list.find(_.code == sicCodeChoice.code).fold(indexes(sicCodeChoice))(choice =>
                  (indexes(choice) ++ indexes(sicCodeChoice)).distinct
                )
              case None => indexes(sicCodeChoice)
            }

          val updates = Seq(
            addEachToSet(s"choices.$n.indexes", combineIndexes(_.indexes): _*),
            addEachToSet(s"choices.$n.indexesCy", combineIndexes(_.indexesCy): _*)
          )

          acc ++ updates
        }

        if (codesToUpdate.isEmpty) {
          addChoices(journeyId, newCodes)
        } else {
          infoLog(s"[SicStoreRepository][insertChoices] Updating SIC code indexes for journey ID $journeyId")
          collection.updateOne(
            filter = journeyIdSelector(journeyId),
            update = combine(currentDate("lastUpdated"), combine(updateIndexes: _*)),
            options = UpdateOptions().upsert(true)
          )
            .toFuture()
            .flatMap(_ => addChoices(journeyId, newCodes))
        }
      }
    }
  }

  def removeChoice(journeyId: String, sicCode: String)(implicit request: Request[_]): Future[Boolean] = {
    retrieveSicStore(journeyId) flatMap {
      case Some(_) =>
        infoLog(s"[SicStoreRepository][removeChoice] Updating SIC code choices for journey ID $journeyId")
        collection.updateOne(
          filter = journeyIdSelector(journeyId),
          update = combine(currentDate("lastUpdated"), pull("choices", Document("code" -> sicCode)))
        ).toFuture()
          .map(_.getModifiedCount == 1)
      case None =>
        warnLog(s"[SicStoreRepository][removeChoice] Failed to find SicStore with journey ID $journeyId")
        Future.successful(false)
    }
  }
}

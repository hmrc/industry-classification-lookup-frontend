/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.Logger
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONLong}
import reactivemongo.core.commands.DeleteIndex
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait TTLIndexing[A, ID] {
  self: ReactiveRepository[A, ID] =>

  val ttl: Long

  private val collectionName: String = self.collection.name

  private val LAST_UPDATED_INDEX = "lastUpdatedIndex"
  private val EXPIRE_AFTER_SECONDS = "expireAfterSeconds"

  def ensureTTLIndexes(implicit ec: scala.concurrent.ExecutionContext): Future[Seq[Boolean]] = {

    collection.indexesManager.list().flatMap {
      indexes => {

        val ttlIndex: Option[Index] = indexes.find(_.eventualName == LAST_UPDATED_INDEX)

        ttlIndex match {
          case Some(index) if hasSameTTL(index) =>
            Logger.info(s"[TTLIndex] document expiration value for collection : $collectionName has not been changed")
            doNothing
          case Some(index) =>
            Logger.info(s"[TTLIndex] document expiration value for collection : $collectionName has been changed. Updating ttl index to : $ttl")
            deleteIndex(index) flatMap(_ => ensureLastUpdated)
          case _ =>
            Logger.info(s"[TTLIndex] TTL Index for collection : $collectionName does not exist. Creating TTL index")
            ensureLastUpdated
        }
      }
    } recoverWith errorHandler
  }

  private val doNothing = Future(Seq(true))

  private def hasSameTTL(index: Index): Boolean = index.options.getAs[BSONLong](EXPIRE_AFTER_SECONDS).exists(_.as[Long] == ttl)

  private def deleteIndex(index: Index) = collection.db.command(DeleteIndex(collection.name, index.eventualName))

  private def errorHandler: PartialFunction[Throwable, Future[Seq[Boolean]]] = {
    case ex =>
      Logger.error(s"[TTLIndex] Exception thrown in TTLIndexing", ex)
      throw ex
  }

  private def ensureLastUpdated: Future[Seq[Boolean]] = {
    Future.sequence(Seq(collection.indexesManager.ensure(
      Index(
        key = Seq("lastUpdated" -> IndexType.Ascending),
        name = Some(LAST_UPDATED_INDEX),
        options = BSONDocument(EXPIRE_AFTER_SECONDS -> BSONLong(ttl))
      )
    ))).map { ensured =>
      Logger.info(s"[TTLIndex] Ensuring ttl index on field : $LAST_UPDATED_INDEX in collection : $collectionName is set to $ttl")
      ensured
    }
  }
}

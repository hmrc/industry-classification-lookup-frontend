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

package repositories

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject.{Inject, Singleton}
import models.setup.{Identifiers, JourneyData, JourneySetup}
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataRepository @Inject()(config: Configuration,
                                      mongo: ReactiveMongoComponent
                                     )(implicit val ec: ExecutionContext)
  extends ReactiveRepository[JourneyData, BSONObjectID](
    "journey-data",
    mongo.mongoConnector.db,
    JourneyData.format
  ) with TTLIndexing[JourneyData, BSONObjectID] {

  override val ttl: Long = config.get[Long]("mongodb.timeToLiveInSeconds")

  override def ensureIndexes(implicit ec: ExecutionContext): Future[Seq[Boolean]] = {
    super.ensureIndexes flatMap { l =>
      ensureTTLIndexes map {
        ttl => l ++ ttl
      }
    }
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq(
        "identifiers.journeyId" -> IndexType.Ascending,
        "identifiers.sessionId" -> IndexType.Ascending
      ),
      name = Some("SessionIdAndJourneyId"),
      unique = true
    )
  )

  private def identifiersSelector(identifiers: Identifiers) = BSONDocument("identifiers.journeyId" -> identifiers.journeyId, "identifiers.sessionId" -> identifiers.sessionId)

  private[repositories] def renewJourney[T](identifiers: Identifiers)(f: => T): Future[T] = {
    val selector = identifiersSelector(identifiers)
    val dateTime = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli
    collection.update.one(selector, BSONDocument("$set" -> BSONDocument("lastUpdated" -> BSONDocument("$date" -> dateTime)))) map { _ =>
      f
    }
  }

  def upsertJourney(journeyData: JourneyData): Future[JourneyData] = {
    val selector = identifiersSelector(journeyData.identifiers)
    collection.findAndUpdate(selector, journeyData, upsert = true, fetchNewObject = true)
      .map(_.result[JourneyData].get)
      .recover { case e =>
        logger.warn(s"[JourneyDataMongoRepository][upsertJourney] failed with message ${e.getMessage} for journeyId: ${journeyData.identifiers.journeyId} sessionId: ${journeyData.identifiers.sessionId}")
        throw e
      }
  }

  def updateJourneySetup(identifiers: Identifiers, journeySetup: JourneySetup): Future[JourneySetup] = {
    implicit val journeySetupWrites: Writes[JourneySetup] = JourneySetup.mongoWrites
    val selector = identifiersSelector(identifiers)
    val journeyDoc = Json.toJson(journeySetup)
    val modifier = Json.obj("$set" -> journeyDoc)
    collection.update.one(selector, modifier).map(res => if (res.n > 0) {
      journeySetup
    } else {
      logger.warn(s"[JourneyDataMongoRepository][updateJourneySetup] completed an update but no document was modified for journeyId: ${identifiers.journeyId} sessionId: ${identifiers.sessionId}")
      throw new RuntimeException("Exception thrown because expected update did not succeed")
    })
      .recover {
        case e =>
          logger.warn(s"[JourneyDataMongoRepository][updateJourneySetup] failed with message: ${e.getMessage} journeyId: ${identifiers.journeyId} sessionId: ${identifiers.sessionId}")
          throw e
      }
  }

  def retrieveJourneyData(identifiers: Identifiers): Future[JourneyData] = {
    val selector = identifiersSelector(identifiers)
    collection.find(selector).one[JourneyData] flatMap {
      case Some(data) => renewJourney(identifiers)(data)
      case _ =>
        logger.warn("Could not find JourneyId")
        throw new RuntimeException("Missing document")
    }
  }
}
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

import com.mongodb.client.model.Filters._
import com.mongodb.client.model.Indexes.ascending
import models.setup.{Identifiers, JourneyData, JourneySetup}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{currentDate, set}
import org.mongodb.scala.model.{IndexOptions, ReplaceOptions}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyDataRepository @Inject()(config: Configuration,
                                      mongo: MongoComponent
                                     )(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[JourneyData](
    mongoComponent = mongo,
    collectionName = "journey-data",
    domainFormat = JourneyData.format,
    indexes = Seq(
      model.IndexModel(
        keys = ascending("identifiers.journeyId", "identifiers.sessionId"),
        indexOptions = IndexOptions()
          .name("SessionIdAndJourneyId")
          .unique(true)
      ),
      model.IndexModel(
        keys = ascending("lastUpdated"),
        indexOptions = IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
      )
    )
  ) with Logging {

  private def identifiersSelector(identifiers: Identifiers): Bson =
    and(equal("identifiers.journeyId", identifiers.journeyId), equal("identifiers.sessionId", identifiers.sessionId))

  private[repositories] def renewJourney[T](identifiers: Identifiers)(f: => T): Future[T] =
    collection.updateOne(
      filter = identifiersSelector(identifiers),
      update = currentDate("lastUpdated")
    )
    .toFuture()
    .map( _ => f)

  def upsertJourney(journeyData: JourneyData): Future[JourneyData] =
    collection.replaceOne(
      filter = identifiersSelector(journeyData.identifiers),
      replacement = journeyData,
      options = ReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => journeyData)
    .recover { case e =>
      logger.warn(s"""[JourneyDataMongoRepository][upsertJourney] failed with message ${e.getMessage}
        for journeyId: ${journeyData.identifiers.journeyId} sessionId: ${journeyData.identifiers.sessionId}""")
      throw e
    }

  def updateJourneySetup(identifiers: Identifiers, journeySetup: JourneySetup): Future[JourneySetup] = {
    collection.updateOne(
      filter = identifiersSelector(identifiers),
      update = set("journeySetupDetails", Codecs.toBson(journeySetup)(JourneySetup.mongoWrites))
    )
    .toFuture()
    .map(res =>
      if (res.getMatchedCount > 0) {
        journeySetup
      } else {
        logger.warn(s"[JourneyDataMongoRepository][updateJourneySetup] completed an update but no document was modified for journeyId: ${identifiers.journeyId} sessionId: ${identifiers.sessionId}")
        throw new RuntimeException("Exception thrown because expected update did not succeed")
      }
    )
    .recover {
      case e =>
        logger.warn(s"[JourneyDataMongoRepository][updateJourneySetup] failed with message: ${e.getMessage} journeyId: ${identifiers.journeyId} sessionId: ${identifiers.sessionId}")
        throw e
    }
  }

  def retrieveJourneyData(identifiers: Identifiers): Future[JourneyData] =
    collection.find[JourneyData](identifiersSelector(identifiers))
      .headOption()
      .flatMap {
        case Some(data) =>
          renewJourney(identifiers)(data)
        case _ =>
          logger.warn("Could not find JourneyId")
          throw new RuntimeException(s"Missing document for journeyId: ${identifiers.journeyId} sessionId: ${identifiers.sessionId}")
      }

}

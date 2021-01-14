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

package helpers.mocks

import models.setup.JourneyData
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import reactivemongo.api.commands.DefaultWriteResult
import repositories.JourneyDataRepository

import scala.concurrent.Future

trait MockJourneyDataRepo extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockJourneyDataRepository: JourneyDataRepository = mock[JourneyDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockJourneyDataRepository)
  }

  def mockWR(success: Boolean) = DefaultWriteResult(success, 1, Seq.empty, None, None, None)

  def mockInitialiseJourney(journeyData: JourneyData): OngoingStubbing[Future[JourneyData]] = {
    when(mockJourneyDataRepository.upsertJourney(ArgumentMatchers.any()))
      .thenReturn(Future.successful(journeyData))
  }

}

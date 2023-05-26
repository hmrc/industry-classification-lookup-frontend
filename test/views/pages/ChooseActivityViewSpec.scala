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

package views.pages

import forms.chooseactivity.ChooseMultipleActivitiesForm
import forms.sicsearch.SicSearchForm
import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models.{SearchResults, Sector, SicCode, SicSearch}
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.chooseActivity

class ChooseActivityViewSpec extends UnitTestSpec with GuiceOneAppPerSuite with MockAppConfig with MockMessages with I18nSupport {

  override def messagesApi: MessagesApi = mockMessagesApi

  implicit val lang: Lang = Lang.defaultLang

  val query = "test query"

  val testSicCode = SicCode("12345", "Testing", "Testing")

  val searchResults = SearchResults(query, 1, List(testSicCode), List(Sector("A", "Fake Sector", "Cy business sector", 1)))

  val journeyId = "testJourneyId"

  val pageHeading = "Standard Industry Classifications (SIC) codes"
  val pageTitle = s"$pageHeading - Register for VAT - GOV.UK"
  val back = "Back"

  "The choose activity screen" should {
    lazy val view = app.injector.instanceOf[chooseActivity].apply(journeyId, SicSearchForm.form.fill(SicSearch(query)), ChooseMultipleActivitiesForm.form(None), Some(searchResults))
    lazy val document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.getElementsByTag("title").first().text mustBe s"1 results - $pageTitle"
    }

    "have the correct title when result is empty" in {
      val viewWithNoResult =
        app.injector.instanceOf[chooseActivity].apply(
          journeyId,
          SicSearchForm.form.fill(SicSearch(query)),
          ChooseMultipleActivitiesForm.form(None),
          Some(searchResults.copy(numFound = 0))
        )
      Jsoup.parse(viewWithNoResult.body).getElementsByTag("title").first().text mustBe s"0 results - $pageTitle"
    }

    "have a back link" in {
      document.select(".govuk-back-link").first().text mustBe back
    }

    "have the correct search terms displayed in the search bar" in {
      document.getElementById("sicSearch").attr("value") mustBe query
    }
  }
}

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

package views.pages

import helpers.UnitTestSpec
import helpers.mocks.{MockAppConfig, MockMessages}
import models.SicCodeChoice
import models.setup.messages.Summary
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.test.FakeRequest
import views.html.pages.{confirmation => ConfirmationPage}

class ConfirmationViewSpec extends UnitTestSpec with GuiceOneAppPerSuite with MockAppConfig with MockMessages with I18nSupport {
  implicit val request: FakeRequest[_] = FakeRequest()

  override def messagesApi: MessagesApi = mockMessagesApi

  val sicCodeChoices = List(
    SicCodeChoice("12345", "my fake description", "my fake description", List("this is the index")),
    SicCodeChoice("67892", "my fake second description", "my fake second description", List("this is index", "with another one"))
  )

  val pageHeading = "MyPageHeading"
  val pageTitle = s"$pageHeading - Register for VAT - GOV.UK"
  val pageLeadParagraph = "MyPageLead"
  val pageHintText = "MyPageHint"

  "The choose activity screen" should {
    lazy val view = app.injector.instanceOf[ConfirmationPage]
    lazy val dynamicView = view(
      "testJourneyId",
      sicCodeChoices,
      None,
      Summary(Some(pageHeading), Some(pageLeadParagraph), Some(pageHintText))
    )

    lazy val defaultView = view(
      "testJourneyId",
      sicCodeChoices,
      None,
      Summary(None, None, None)
    )

    lazy val dynamicDocument = Jsoup.parse(dynamicView.body)
    lazy val defaultDocument = Jsoup.parse(defaultView.body)

    "have the correct content" when {
      "dynamic content is passed in" in {
        dynamicDocument.getElementsByTag("title").first().text mustBe pageTitle
        dynamicDocument.select(".govuk-back-link").first().text mustBe "Back"
        dynamicDocument.getElementById("page-lead-text").text() mustBe pageLeadParagraph
        dynamicDocument.getElementById("page-hint-text").text() mustBe pageHintText
      }
      "no content is passed in" in {
        defaultDocument.getElementsByTag("title").first().text mustBe "Check and confirm the business's Standard Industry Classification (SIC) codes - Register for VAT - GOV.UK"
        defaultDocument.select(".govuk-back-link").first().text mustBe "Back"
        Option(defaultDocument.getElementById("page-lead-text")) mustBe None
        Option(defaultDocument.getElementById("page-hint-text")) mustBe None
      }
    }
  }

}

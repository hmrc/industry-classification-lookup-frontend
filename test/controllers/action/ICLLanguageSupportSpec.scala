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

package controllers.action

import config.AppConfig
import featureswitch.core.config.{FeatureSwitching, WelshLanguage}
import helpers.UnitTestSpec
import models.SicCodeChoice
import org.jsoup.Jsoup
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.test.FakeRequest
import views.html.pages.confirmation

import javax.inject.Inject

class VatRegLanguageSupportSpec extends UnitTestSpec with FeatureSwitching {

  lazy val app: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> "false")
    .build()

  object ExpectedMessages {
    val welshHeading = "Gwirio a chadarnhau codau Dosbarthiad Diwydiannol Safonol (SIC) y busnes"
    val englishHeading = "Check and confirm the business's Standard Industry Classification (SIC) codes"
  }

  val testController: WelshTestController = app.injector.instanceOf[WelshTestController]

  "ICL Language Support" when {
    "the play lang cookie is set to Welsh" when {
      "the Welsh feature switch is enabled" must {
        "return a page with Welsh content" in {
          enable(WelshLanguage)

          val res = testController.getPage()(FakeRequest().withCookies(Cookie("PLAY_LANG", "cy")))
          val doc = Jsoup.parse(contentAsString(res))

          doc.select("h1").first().text() mustBe ExpectedMessages.welshHeading
        }
      }
      "the Welsh feature switch is disabled" must {
        "return a page with English content" in {
          disable(WelshLanguage)

          val res = testController.getPage()(FakeRequest().withCookies(Cookie("PLAY_LANG", "cy")))
          val doc = Jsoup.parse(contentAsString(res))

          doc.select("h1").first().text() mustBe ExpectedMessages.englishHeading
        }
      }
    }
    "the play lang cookie is set to English" when {
      "the Welsh feature switch is enabled" must {
        "return a page with English content" in {
          enable(WelshLanguage)

          val res = testController.getPage()(FakeRequest().withCookies(Cookie("PLAY_LANG", "en")))
          val doc = Jsoup.parse(contentAsString(res))

          doc.select("h1").first().text() mustBe ExpectedMessages.englishHeading
        }
      }
      "the Welsh feature switch is disabled" must {
        "return a page with English content" in {
          disable(WelshLanguage)

          val res = testController.getPage()(FakeRequest().withCookies(Cookie("PLAY_LANG", "en")))
          val doc = Jsoup.parse(contentAsString(res))

          doc.select("h1").first().text() mustBe ExpectedMessages.englishHeading
        }
      }
    }
  }

}

class WelshTestController @Inject()(page: confirmation,
                                    val controllerComponents: ControllerComponents)
                                   (implicit appConfig: AppConfig) extends BaseController with ICLLanguageSupport {

  def getPage: Action[AnyContent] = Action { request =>
    implicit val cacheRequest: WrappedRequest[AnyContent] = new WrappedRequest[AnyContent](request)
    Ok(page("1", List.empty[SicCodeChoice]))
  }

}

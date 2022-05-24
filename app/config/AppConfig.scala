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

package config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.nio.charset.Charset
import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(configuration: ServicesConfig) {

  def loadConfig(key: String): String = configuration.getString(key)

  lazy val industryClassificationLookupBackend: String = configuration.baseUrl("industry-classification-lookup")

  private lazy val contactHost = configuration.getString(s"contact-frontend.host")
  private val contactFormServiceIdentifier = "vrs"

  lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  lazy val analyticsHost: String = loadConfig(s"google-analytics.host")
  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"

  lazy val feedbackFrontendUrl = loadConfig("microservice.services.feedback-frontend.url")
  lazy val signOutUrl = s"$feedbackFrontendUrl/feedback/vat-registration"

  lazy val countdownLength: Int = configuration.getInt("timeout.countdown")
  lazy val timeoutLength: Int = configuration.getInt("timeout.length")

  lazy val accessibilityStatementUrl: String = configuration.getString("accessibility-statement.host") +
    "/accessibility-statement" + configuration.getString("accessibility-statement.service-path")
}
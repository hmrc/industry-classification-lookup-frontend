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

import featureswitch.core.config.FeatureSwitching
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(configuration: ServicesConfig) extends FeatureSwitching {

  def loadConfig(key: String): String = configuration.getString(key)

  lazy val baseUrl = {
    val localFrontendService = "microservice.services.industry-classification-lookup-frontend"
    val host = configuration.getString(s"$localFrontendService.host")
    val port = configuration.getString(s"$localFrontendService.port")
    val protocol = configuration.getString(s"$localFrontendService.protocol")

    s"$protocol://$host:$port"
  }
  lazy val industryClassificationLookupBackend: String = configuration.baseUrl("industry-classification-lookup")
  private lazy val contactHost = configuration.getString(s"microservice.services.contact-frontend.host")
  private val contactFormServiceIdentifier = "vrs"
  lazy val betaFeedbackUrl = s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier"
  lazy val feedbackFrontendUrl = loadConfig("microservice.services.feedback-frontend.url")
  lazy val signOutUrl = s"$feedbackFrontendUrl/feedback/vat-registration"
  lazy val countdownLength: Int = configuration.getInt("timeout.countdown")
  lazy val timeoutLength: Int = configuration.getInt("timeout.length")
  lazy val accessibilityStatementUrl: String = configuration.getString("accessibility-statement.host") +
    "/accessibility-statement" + configuration.getString("accessibility-statement.service-path")

}
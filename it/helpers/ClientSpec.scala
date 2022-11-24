/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers

import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.{Assertion, BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.Future

trait ClientSpec extends PlaySpec with GuiceOneServerPerSuite with Wiremock with TestAppConfig
  with FutureAwaits with DefaultAwaitTimeout with HeaderNames
  with BeforeAndAfterEach with BeforeAndAfterAll with LoginStub with ICLStub with PatienceConfiguration with IntegrationPatience {

  def buildClient(path: String)(implicit app: Application): WSRequest = {
    app.injector.instanceOf[WSClient]
      .url(s"http://localhost:$port$path")
      .withFollowRedirects(false)
  }

  def assertFutureResponse(func: => Future[WSResponse])(assertions: WSResponse => Assertion): Assertion = {
    assertions(await(func))
  }

  override def beforeEach(): Unit = resetWiremock()

  override def beforeAll(){
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(){
    stopWiremock()
    super.afterAll()
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
      "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
      "auditing.consumer.baseUri.host" -> s"$wiremockHost",
      "auditing.consumer.baseUri.port" -> s"$wiremockPort",
      "microservice.services.industry-classification-lookup.port" -> s"$wiremockPort",
      "microservice.services.industry-classification-lookup.host" -> s"$wiremockHost",
      "microservice.services.cachable.session-cache.host" -> s"$wiremockHost",
      "microservice.services.cachable.session-cache.port" -> s"$wiremockPort",
      "microservice.services.cachable.session-cache.domain" -> "keystore",
      "microservice.services.cachable.short-lived-cache.host" -> s"$wiremockHost",
      "microservice.services.cachable.short-lived-cache.port" -> s"$wiremockPort",
      "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
      "microservice.services.auth.host" -> s"$wiremockHost",
      "microservice.services.auth.port" -> s"$wiremockPort"
    ).build()
}


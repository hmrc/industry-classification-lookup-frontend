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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json

trait Wiremock extends WiremockHelpers {
  self: GuiceOneServerPerSuite =>

  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val wiremockUrl = s"http://$wiremockHost:$wiremockPort"

  private val config: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  private val wireMockServer = new WireMockServer(config)

  def startWiremock() {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()
}

trait WiremockHelpers {
  this: Wiremock =>

  def stubGet(url: String, status: Integer, body: Option[String]): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn {
        val resp = aResponse().withStatus(status)
        body.fold(resp)(resp.withBody)
      }
    )

  def stubPost(url: String, status: Integer, body: Option[String]): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn {
        val resp = aResponse().withStatus(status)
        body.fold(resp)(resp.withBody)
      }
    )

  def stubPut(url: String, status: Integer, body: Option[String]): StubMapping =
    stubFor(put(urlMatching(url))
      .willReturn {
        val resp = aResponse().withStatus(status)
        body.fold(resp)(resp.withBody)
      }
    )

  def mockAuthorise(status: Int = 200, resp: Option[String] = None): StubMapping = stubPost(
    url = "/auth/authorise",
    status = status,
    body = resp match {
      case Some(_) => resp
      case None    => Some(Json.obj("authorise" -> Json.arr(), "retrieve" -> Json.arr()).toString())
    }
  )
}

/*
 * Copyright 2018 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._

trait LoginStub {
  def stubSuccessfulLogin(withSignIn: Boolean = false) = {

    stubFor(get(urlEqualTo("/auth/authority"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
               |{
               |    "uri": "/auth/oid/1234567890",
               |    "loggedInAt": "2014-06-09T14:57:09.522Z",
               |    "previouslyLoggedInAt": "2014-06-09T14:48:24.841Z",
               |    "accounts": {
               |    },
               |    "levelOfAssurance": "2",
               |    "confidenceLevel" : 50,
               |    "credentialStrength": "strong",
               |    "userDetailsLink": "/user-details/id/1234567890",
               |    "ids": "/auth/oid/1234567890/ids",
               |    "legacyOid":"1234567890"
               |}
               |
            """.stripMargin
          )))

    stubFor(get(urlMatching("/user-details/id/1234567890"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(s"""{
                      |  "name": "testUserName",
                      |  "email": "testUserEmail",
                      |  "affinityGroup": "testAffinityGroup",
                      |  "authProviderId": "testAuthProviderId",
                      |  "authProviderType": "testAuthProviderType"
                      |}""".stripMargin)
      )
    )

    stubFor(get(urlMatching("/auth/oid/1234567890/ids"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
      )
    )
  }

  def setupSimpleAuthMocks() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )

    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody("""{"internalId": "Int-xxx"}""")
      )
    )

    stubFor(get(urlMatching("/auth/ids"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
      )
    )
  }

  def setupUnauthorised() = {
    stubFor(post(urlMatching("/write/audit"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody("""{"x":2}""")
      )
    )

    stubFor(post(urlMatching("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(404)
      )
    )
  }
}

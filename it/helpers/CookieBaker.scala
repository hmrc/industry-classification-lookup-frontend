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

import play.api.Application
import play.api.libs.crypto.CookieSigner
import play.api.libs.ws.WSCookie
import uk.gov.hmrc.crypto.{Crypted, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.http.SessionKeys

import java.net.{URLDecoder, URLEncoder}

object CookieBaker extends ClientSpec {

  val cookieKey: String = "gvBoGdgzqG1AarzF1LY0zQ=="
  val userIdKey: String = "userId"
  val tokenKey: String = "token"
  val referenceKey: String = "reference"
  val defaultSessionId: String = "session-ac4ed3e7-dbc3-4150-9574-40771c4285c1"

  def cookieValue(sessionData: Map[String, String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")

      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      val cookieSignerCache: Application => CookieSigner = Application.instanceCache[CookieSigner]
      def cookieSigner: CookieSigner = cookieSignerCache(app)

      PlainText(cookieSigner.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = SymmetricCryptoFactory.aesGcmCrypto(cookieKey).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  def getCookieData(cookie: WSCookie): Map[String, String] = {
    getCookieData(cookie.value)
  }

  def getCookieData(cookieData: String): Map[String, String] = {
    val decrypted = SymmetricCryptoFactory.aesGcmCrypto(cookieKey).decrypt(Crypted(cookieData)).value
    val result = decrypted.split("&")
      .map(_.split("="))
      .map { case Array(k, v) => (k, URLDecoder.decode(v, java.nio.charset.Charset.defaultCharset().name())) }
      .toMap

    result
  }

  def cookieData(userId: String = "anyUserId", sessionID: Option[String] = None): Map[String, String] = {
    Map(
      SessionKeys.authToken -> "test",
      SessionKeys.sessionId -> sessionID.getOrElse(defaultSessionId),
      tokenKey -> "RANDOMTOKEN",
      userIdKey -> userId
    )
  }

  def getSessionCookie(sessionID: Option[String] = None) = {
    cookieValue(cookieData(sessionID = sessionID))
  }

}

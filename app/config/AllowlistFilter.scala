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

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import uk.gov.hmrc.allowlist.AkamaiAllowlistFilter

@Singleton
class AllowlistFilter @Inject()(val mat: Materializer,
                                appConfig: AppConfig)
  extends AkamaiAllowlistFilter {

  override def allowlist: Seq[String] = appConfig.allowlist

  override def excludedPaths: Seq[Call] = {
    appConfig.allowlistExcluded.map { path =>
      Call("GET", path)
    }
  }

  override def destination: Call = Call("GET", "https://www.tax.service.gov.uk/outage-sic-search")
}

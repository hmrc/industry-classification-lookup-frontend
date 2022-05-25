
package helpers

import config.AppConfig
import fixtures.BaseA11yFixtures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.scalatestaccessibilitylinter.AccessibilityMatchers

trait A11ySpec extends AnyWordSpec
  with BaseA11yFixtures
  with Matchers
  with GuiceOneAppPerSuite
  with AccessibilityMatchers {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val config: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  implicit val messages: Messages = messagesApi.preferred(Seq(lang))

}

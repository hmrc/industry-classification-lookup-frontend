
package pages

import forms.RemoveSicCodeForm
import helpers.A11ySpec
import play.api.data.Form
import views.html.pages.confirmation

class ConfirmationA11ySpec extends A11ySpec {

  val view: confirmation = app.injector.instanceOf[confirmation]

  "the Confirmation page" when {
    "the page is rendered without errors" must {
      "pass all accessibility tests" in {
        view(testJourneyId, List(testSicCodeChoice)).toString must passAccessibilityChecks
      }
    }

    "the page is rendered with errors" must {
      "pass all accessibility tests" in {
        view(testJourneyId, List(testSicCodeChoice), Some(Seq("1"))).toString must passAccessibilityChecks
      }
    }
  }

}

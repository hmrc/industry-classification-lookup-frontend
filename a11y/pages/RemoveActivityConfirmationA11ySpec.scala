
package pages

import forms.RemoveSicCodeForm
import helpers.A11ySpec
import play.api.data.Form
import views.html.pages.removeActivityConfirmation

class RemoveActivityConfirmationA11ySpec extends A11ySpec {

  val view: removeActivityConfirmation = app.injector.instanceOf[removeActivityConfirmation]
  val form: Form[String] = RemoveSicCodeForm.form(testSicDescription)

  "the Remove Sic Code page" when {
    "the page is rendered without errors" must {
      "pass all accessibility tests" in {
        view(testJourneyId, form, testSicCodeChoice).toString must passAccessibilityChecks
      }
    }

    "the page is rendered with errors" must {
      "pass all accessibility tests" in {
        view(testJourneyId, form.bind(Map("removeCode" -> "")), testSicCodeChoice).toString must passAccessibilityChecks
      }
    }
  }

}

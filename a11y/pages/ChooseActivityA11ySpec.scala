
package pages

import forms.RemoveSicCodeForm
import forms.chooseactivity.ChooseMultipleActivitiesForm
import forms.sicsearch.SicSearchForm
import helpers.A11ySpec
import models.{SearchResults, SicCode, SicSearch}
import play.api.data.Form
import views.html.pages.chooseActivity

class ChooseActivityA11ySpec extends A11ySpec {

  val view: chooseActivity = app.injector.instanceOf[chooseActivity]
  val searchForm: Form[SicSearch] = SicSearchForm.form
  val chooseActivityForm: Form[List[SicCode]] = ChooseMultipleActivitiesForm.form

  val testQuery = "testQuery"
  val testSearchResults: SearchResults = SearchResults(testQuery, 1, List(testSicCode), List(testSector))
  val testSearchResultsWithSector: SearchResults = SearchResults(testQuery, 1, List(testSicCode), List(testSector), Some(testSector))
  val searchFormWithQuery: Form[SicSearch] = SicSearchForm.form.fill(SicSearch(testQuery))

  "the Choose Activity page" when {
    "there are no search results" when {
      "the page is rendered without errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchForm, chooseActivityForm, None).toString must passAccessibilityChecks
        }
      }

      "the page is rendered with errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchForm.bind(Map("sicSearch" -> "")), chooseActivityForm, None).toString must passAccessibilityChecks
        }
      }
    }

    "there are search results" when {
      "the page is rendered without errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchFormWithQuery, chooseActivityForm, Some(testSearchResults)).toString must passAccessibilityChecks
        }
      }

      "the page is rendered with errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchFormWithQuery, chooseActivityForm.bind(Map("code" -> "")), Some(testSearchResults)).toString must passAccessibilityChecks
        }
      }
    }

    "there are search results with a selected sector" when {
      "the page is rendered without errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchFormWithQuery, chooseActivityForm, Some(testSearchResultsWithSector)).toString must passAccessibilityChecks
        }
      }

      "the page is rendered with errors" must {
        "pass all accessibility tests" in {
          view(testJourneyId, searchFormWithQuery, chooseActivityForm.bind(Map("code" -> "")), Some(testSearchResultsWithSector)).toString must passAccessibilityChecks
        }
      }
    }
  }

}

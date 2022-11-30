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

package controllers

import config.AppConfig
import forms.chooseactivity.ChooseMultipleActivitiesForm
import forms.sicsearch.SicSearchForm
import models.setup.{Identifiers, JourneyData}
import models.{SearchResults, SicCode, SicSearch}
import play.api.data.Form
import play.api.mvc._
import services.{JourneyService, SicSearchService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import views.html.pages.chooseActivity

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChooseActivityController @Inject()(mcc: MessagesControllerComponents,
                                         val sicSearchService: SicSearchService,
                                         val journeyService: JourneyService,
                                         val authConnector: AuthConnector,
                                         view: chooseActivity)
                                        (implicit ec: ExecutionContext, val appConfig: AppConfig) extends ICLController(mcc) {

  val chooseActivityForm: Form[List[SicCode]] = ChooseMultipleActivitiesForm.form(None)

  def show(journeyId: String, doSearch: Option[Boolean] = None): Action[AnyContent] = Action.async {
    implicit request =>
      userAuthorised() {
        withJourney(journeyId) { journeyData =>
          if (doSearch.contains(true)) {
            withSearchResults(journeyData.identifiers) { searchResults =>
              Future.successful(Ok(view(journeyId, SicSearchForm.form.fill(SicSearch(searchResults.query)), chooseActivityForm, Some(searchResults))))
            }
          } else {
            Future.successful(Ok(view(journeyId, SicSearchForm.form, chooseActivityForm, None)))
          }
        }
      }
  }

  def submit(journeyId: String, doSearch: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>
    userAuthorised() {
      withJourney(journeyId) { journeyData =>
        doSearch.fold(selectActivities(journeyId, journeyData))(_ => performSearch(journeyId, journeyData))
      }
    }
  }

  def filter(journeyId: String, sectorCode: String): Action[AnyContent] = Action.async { implicit request =>
    filterResultsBySector(journeyId, Some(sectorCode))
  }

  def clearFilter(journeyId: String): Action[AnyContent] = Action.async { implicit request =>
    filterResultsBySector(journeyId, None)
  }

  private[controllers] def performSearch(journeyId: String, journeyData: JourneyData)(implicit request: Request[_]): Future[Result] = {
    SicSearchForm.form.bindFromRequest.fold(
      errors =>
        Future.successful(BadRequest(view(journeyId, errors, chooseActivityForm, None))),
      form => {
        sicSearchService.search(journeyData, form.sicSearch, None, getLang).map {
          case 1 => Redirect(routes.ConfirmationController.show(journeyId))
          case _ => Redirect(routes.ChooseActivityController.show(journeyId, doSearch = Some(true)))
        }
      }
    )
  }

  private[controllers] def selectActivities(journeyId: String, journeyData: JourneyData)(implicit request: Request[_]): Future[Result] = {
    withSearchResults(journeyData.identifiers) { searchResults =>
      ChooseMultipleActivitiesForm.form(Some(searchResults)).bindFromRequest().fold(
        errors =>
          Future.successful(BadRequest(view(journeyId, SicSearchForm.form.fill(SicSearch(searchResults.query)), errors, Some(searchResults)))),
        codes =>
          sicSearchService.lookupSicCodes(journeyData, codes) map { _ =>
            Redirect(routes.ConfirmationController.show(journeyId))
          }
      )
    }
  }

  private def filterResultsBySector(journeyId: String, sectorCode: Option[String])(implicit hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
    userAuthorised() {
      withJourney(journeyId) { journeyData =>
        withSearchResults(journeyData.identifiers) { searchResults =>
          sicSearchService.search(journeyData, searchResults.query, sectorCode, getLang).map { _ =>
            Redirect(routes.ChooseActivityController.show(journeyId, doSearch = Some(true)))
          }
        }
      }
    }
  }

  private[controllers] def withSearchResults(identifiers: Identifiers)(f: => SearchResults => Future[Result])(implicit request: Request[_]): Future[Result] = {
    sicSearchService.retrieveSearchResults(identifiers.journeyId) flatMap {
      case Some(searchResults) => f(searchResults)
      case None => Future.successful(Ok(view(identifiers.journeyId, SicSearchForm.form, chooseActivityForm, None)))
    }
  }
}
/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, OtherEntityTypeAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.navigation.OtherEntityCheckYourAnswersPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.OtherEntityCheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OtherEntityCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  pageNavigator: OtherEntityCheckYourAnswersPageNavigator,
  val controllerComponents: MessagesControllerComponents,
  checkIfOtherEntityTypeEnabled: OtherEntityTypeAction,
  view: OtherEntityCheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def otherEntityDetails()(implicit request: RegistrationDataRequest[_]): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        OtherEntityTypeSummary.row(),
        BusinessNameSummary.row(),
        CharityRegistrationNumberSummary.row(),
        CompanyRegistrationNumberSummary.row(),
        DoYouHaveCtUtrSummary.row(),
        UtrTypeSummary.row(),
        OtherEntitySaUtrSummary.row(),
        OtherEntityCtUtrSummary.row(),
        OtherEntityPostcodeSummary.row(),
        OverseasTaxIdentifierSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def onPageLoad(): Action[AnyContent] =
    (checkIfOtherEntityTypeEnabled andThen authorise andThen getRegistrationData) { implicit request =>
      Ok(view(otherEntityDetails()))
    }

  def onSubmit(): Action[AnyContent] =
    (checkIfOtherEntityTypeEnabled andThen authorise andThen getRegistrationData).async { implicit request =>
      Future.successful(Redirect(pageNavigator.nextPage(NormalMode, request.registration)))
    }
}

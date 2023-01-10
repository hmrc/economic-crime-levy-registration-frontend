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

import cats.data.Validated.{Invalid, Valid}
import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.services.SubmissionValidationService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CheckYourAnswersView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Singleton

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  submissionValidationService: SubmissionValidationService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    val organisationDetails = SummaryListViewModel(
      rows = Seq(
        EntityTypeSummary.row(),
        AmlSupervisorSummary.row(),
        BusinessSectorSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

    val personalDetails = SummaryListViewModel(
      rows = Seq(
        FirstContactNameSummary.row(),
        FirstContactRoleSummary.row(),
        FirstContactEmailSummary.row(),
        FirstContactNumberSummary.row(),
        SecondContactNameSummary.row(),
        SecondContactRoleSummary.row(),
        SecondContactEmailSummary.row(),
        SecondContactNumberSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

    submissionValidationService.validateRegistrationSubmission() match {
      case Valid(_)   => Ok(view(organisationDetails, personalDetails))
      case Invalid(_) => Redirect(routes.StartController.onPageLoad())
    }

  }
}

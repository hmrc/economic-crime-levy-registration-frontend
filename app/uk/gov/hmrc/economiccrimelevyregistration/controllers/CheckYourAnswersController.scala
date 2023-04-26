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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, ValidatedRegistrationAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.EmailService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.Base64
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  validateRegistrationData: ValidatedRegistrationAction,
  emailService: EmailService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def organisationDetails()(implicit request: RegistrationDataRequest[_]): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        EntityTypeSummary.row(),
        EntityNameSummary.row(),
        CompanyNumberSummary.row(),
        CtUtrSummary.row(),
        SaUtrSummary.row(),
        NinoSummary.row(),
        DateOfBirthSummary.row(),
        AmlRegulatedActivitySummary.row(),
        RelevantAp12MonthsSummary.row(),
        RelevantApLengthSummary.row(),
        UkRevenueSummary.row(),
        AmlSupervisorSummary.row(),
        BusinessSectorSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def contactDetails()(implicit request: RegistrationDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      FirstContactNameSummary.row(),
      FirstContactRoleSummary.row(),
      FirstContactEmailSummary.row(),
      FirstContactNumberSummary.row(),
      SecondContactSummary.row(),
      SecondContactNameSummary.row(),
      SecondContactRoleSummary.row(),
      SecondContactEmailSummary.row(),
      SecondContactNumberSummary.row(),
      UseRegisteredAddressSummary.row(),
      ContactAddressSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen validateRegistrationData) { implicit request =>
      Ok(view(organisationDetails(), contactDetails()))
    }

  def onSubmit(): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val htmlView = view(organisationDetails(), contactDetails())

    val base64EncodedHtmlView: String = base64EncodeHtmlView(htmlView.body)

    for {
      _        <- eclRegistrationConnector.upsertRegistration(registration =
                    request.registration.copy(base64EncodedNrsSubmissionHtml = Some(base64EncodedHtmlView))
                  )
      response <- eclRegistrationConnector.submitRegistration(request.internalId)
      _         = emailService.sendRegistrationSubmittedEmails(request.registration.contacts, response.eclReference)
      _        <- eclRegistrationConnector.deleteRegistration(request.internalId)
    } yield {
      val updatedSession = request.session ++ Seq(
        SessionKeys.EclReference             -> response.eclReference,
        SessionKeys.FirstContactEmailAddress -> request.registration.contacts.firstContactDetails.emailAddress
          .getOrElse(throw new IllegalStateException("First contact email address not found in registration data"))
      )

      Redirect(routes.RegistrationSubmittedController.onPageLoad()).withSession(
        request.registration.contacts.secondContactDetails.emailAddress.fold(updatedSession)(email =>
          updatedSession ++ Seq(SessionKeys.SecondContactEmailAddress -> email)
        )
      )
    }
  }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)
}

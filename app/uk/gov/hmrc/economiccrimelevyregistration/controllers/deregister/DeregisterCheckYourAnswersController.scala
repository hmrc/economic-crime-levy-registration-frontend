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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Base64EncodedFields, ContactDetails, GetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.deregister.DeregistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.deregister._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.SummaryListFluency
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterCheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeregisterCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getDeregistrationData: DeregistrationDataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  deregistrationService: DeregistrationService,
  view: DeregisterCheckYourAnswersView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler
    with SummaryListFluency {

  def onPageLoad(): Action[AnyContent] = (authorise andThen getDeregistrationData).async { implicit request =>
    (for {
      eclReference      <- valueOrError(request.eclRegistrationReference, "ECL reference")
      subscription      <- eclRegistrationService.getSubscription(eclReference).asResponseError
      deregisterHtmlView = createHtmlView(subscription)
      _                 <- deregistrationService
                             .upsert(request.deregistration.copy(eclReference = request.eclRegistrationReference))
                             .asResponseError
    } yield deregisterHtmlView).fold(
      err => routeError(err),
      deregisterCheckYourAnswersView => Ok(deregisterCheckYourAnswersView)
    )
  }

  def onSubmit(): Action[AnyContent] = (authorise andThen getDeregistrationData).async { implicit request =>
    (for {
      eclReference               <- valueOrError(request.eclRegistrationReference, "ECL reference")
      subscription               <- eclRegistrationService.getSubscription(eclReference).asResponseError
      deregisterHtmlView          = createHtmlView(subscription)
      base64EncodedHtmlViewForPdf = base64EncodeHtmlView(deregisterHtmlView.body)
      updatedDeregistration       =
        request.deregistration
          .copy(dmsSubmissionHtml = Some(base64EncodedHtmlViewForPdf))
      _                          <- deregistrationService
                                      .upsert(updatedDeregistration)
                                      .asResponseError
      _                          <- deregistrationService.submit(request.internalId).asResponseError
    } yield ()).fold(
      err => routeError(err),
      _ => Redirect(routes.DeregistrationRequestedController.onPageLoad())
    )
  }

  private def createHtmlView(
    subscription: GetSubscriptionResponse
  )(implicit request: DeregistrationDataRequest[_]) =
    view(
      organisation(
        request.eclRegistrationReference,
        subscription.legalEntityDetails.organisationName
      ),
      additionalInfo(request.deregistration),
      contact(request.deregistration.contactDetails),
      request.deregistration.registrationType
    )

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  def organisation(eclReference: Option[String], companyName: Option[String])(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        EclReferenceSummary.row(eclReference),
        CompanyNameSummary.row(companyName)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def additionalInfo(deregistration: Deregistration)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        DeregisterReasonSummary.row(deregistration.reason),
        DeregisterDateSummary.row(deregistration.date)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contact(contactDetails: ContactDetails)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        DeregisterNameSummary.row(contactDetails.name),
        DeregisterRoleSummary.row(contactDetails.role),
        DeregisterEmailSummary.row(contactDetails.emailAddress),
        DeregisterNumberSummary.row(contactDetails.telephoneNumber)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
}

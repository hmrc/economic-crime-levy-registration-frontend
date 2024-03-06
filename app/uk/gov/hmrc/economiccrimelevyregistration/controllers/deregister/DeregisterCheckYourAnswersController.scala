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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.{DeregisterCheckYourAnswersView, DeregistrationPdfView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeregisterCheckYourAnswersController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getDeregistrationData: DeregistrationDataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  deregistrationService: DeregistrationService,
  emailService: EmailService,
  view: DeregisterCheckYourAnswersView,
  pdfView: DeregistrationPdfView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler
    with DeregisterPdfEncoder {

  def onPageLoad(): Action[AnyContent] = (authorise andThen getDeregistrationData).async { implicit request =>
    (for {
      eclReference <- valueOrError(request.eclRegistrationReference, "ECL reference")
      subscription <- eclRegistrationService.getSubscription(eclReference).asResponseError
      _            <- deregistrationService
                        .upsert(request.deregistration.copy(eclReference = request.eclRegistrationReference))
                        .asResponseError
    } yield subscription).fold(
      err => routeError(err),
      subscription =>
        Ok(
          view(
            organisation(
              request.eclRegistrationReference,
              subscription.legalEntityDetails.organisationName
            ),
            additionalInfo(request.deregistration),
            contact(request.deregistration.contactDetails)
          )
        )
    )
  }

  def onSubmit(): Action[AnyContent] = (authorise andThen getDeregistrationData).async { implicit request =>
    (for {
      eclReference               <- valueOrError(request.eclRegistrationReference, "ECL reference")
      subscription               <- eclRegistrationService.getSubscription(eclReference).asResponseError
      deregisterHtmlView          =
        createPdfView(subscription, pdfView, request.eclRegistrationReference, request.deregistration)
      base64EncodedHtmlViewForPdf = base64EncodeHtmlView(deregisterHtmlView.toString())
      updatedDeregistration       =
        request.deregistration
          .copy(dmsSubmissionHtml = Some(base64EncodedHtmlViewForPdf))
      _                          <- deregistrationService
                                      .upsert(updatedDeregistration)
                                      .asResponseError
      _                          <- deregistrationService.submit(request.internalId).asResponseError
      address                     = subscription.correspondenceAddressDetails
      name                       <- valueOrError(request.deregistration.contactDetails.name, "Name")
      email                      <- valueOrError(request.deregistration.contactDetails.emailAddress, "Email address")
      _                          <- emailService.sendDeregistrationEmail(email, name, eclReference, address).asResponseError
    } yield email).fold(
      err => routeError(err),
      email =>
        Redirect(routes.DeregistrationRequestedController.onPageLoad())
          .withSession(request.session ++ Seq(SessionKeys.EmailAddress -> email))
    )
  }
}

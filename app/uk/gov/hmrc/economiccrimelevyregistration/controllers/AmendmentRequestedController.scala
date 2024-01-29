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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendmentRequestedView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.economiccrimelevyregistration.controllers.BaseController

@Singleton
class AmendmentRequestedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: AmendmentRequestedView,
  authorise: AuthorisedActionWithEnrolmentCheck,
  sessionService: SessionService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      _                        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                        <- registrationService.deleteRegistration(request.internalId).asResponseError
      firstContactEmailAddress <-
        sessionService.get(request.session, request.internalId, SessionKeys.FirstContactEmailAddress).asResponseError
    } yield firstContactEmailAddress).fold(
      error => routeError(error),
      success => Ok(view(success, request.eclRegistrationReference))
    )
  }
}

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

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendmentRequestedView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, SessionKeys}

@Singleton
class AmendmentRequestedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: AmendmentRequestedView,
  authorise: AuthorisedActionWithEnrolmentCheck,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      _              <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _              <- registrationService.deleteRegistration(request.internalId).asResponseError
      email          <- valueOrError(request.session.get(SessionKeys.FirstContactEmail), "First contact email")
      contactAddress <- valueOrError(
                          request.session
                            .get(SessionKeys.ContactAddress),
                          "Contact address in session"
                        )
      eclAddress     <- valueOrError(
                          Json.fromJson[EclAddress](Json.parse(contactAddress)).asOpt,
                          "Contact address parsed from session"
                        )
    } yield (email, eclAddress)).fold(
      error => routeError(error),
      emailAndAddress => {
        val firstContactEmail = emailAndAddress._1
        val address           = emailAndAddress._2
        Ok(
          view(
            firstContactEmail,
            request.eclRegistrationReference,
            address
          )
        )
      }
    )
  }

}

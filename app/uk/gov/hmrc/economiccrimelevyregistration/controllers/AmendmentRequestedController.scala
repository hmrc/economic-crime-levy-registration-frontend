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

import cats.data.EitherT
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendmentRequestedView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.ResponseError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

@Singleton
class AmendmentRequestedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: AmendmentRequestedView,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    (for {
      _               <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _               <- registrationService.deleteRegistration(request.internalId).asResponseError
      emailAndAddress <- retrieveFromRequestOrSession
    } yield emailAndAddress).fold(
      error => routeError(error),
      emailAndAddress => {
        val firstContactEmail = emailAndAddress._1
        val contactAddress    = emailAndAddress._2
        Ok(
          view(
            firstContactEmail,
            request.eclRegistrationReference,
            contactAddress
          )
        ).withSession(
          request.session ++ Map(
            (SessionKeys.FirstContactEmail -> firstContactEmail),
            (SessionKeys.ContactAddress    -> Json.stringify(Json.toJson(contactAddress)))
          )
        )
      }
    )
  }

  private def retrieveFromRequestOrSession(implicit
    request: RegistrationDataRequest[_]
  ): EitherT[Future, ResponseError, (String, EclAddress)] =
    (request.registration.contacts.firstContactDetails.emailAddress, request.registration.contactAddress) match {
      case (Some(email), Some(address)) =>
        EitherT[Future, ResponseError, (String, EclAddress)](Future.successful(Right((email, address))))
      case _                            =>
        for {

          email           <- valueOrError(request.session.get(SessionKeys.FirstContactEmail), "First contact email")
          address         <- valueOrError(
                               request.session
                                 .get(SessionKeys.ContactAddress),
                               "Contact address in session"
                             )
          eclAddressResult = Json.fromJson[EclAddress](Json.parse(address))
          eclAddress      <- valueOrError(
                               eclAddressResult.asOpt,
                               "Contact address parsed from session"
                             )
        } yield (email, eclAddress)

    }

}

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

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.forms.IsUkAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.services.{AddressLookupService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, IsUkAddressView}
import uk.gov.hmrc.http.HttpVerbs.GET
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IsUkAddressController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: IsUkAddressFormProvider,
  view: IsUkAddressView,
  addressLookupService: AddressLookupService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(
      view(
        form.prepare(request.registration.contactAddressIsUk),
        mode,
        request.registration.registrationType,
        request.eclRegistrationReference
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors, mode, request.registration.registrationType, request.eclRegistrationReference)
            )
          ),
        contactAddressIsUk => {
          val updatedRegistration = request.registration.copy(contactAddressIsUk = Some(contactAddressIsUk))
          (for {
            upsertedRegistration <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
            addressLookupUrl     <- addressLookupService.initJourney(contactAddressIsUk, mode).asResponseError
          } yield addressLookupUrl).fold(
            err => Redirect(routes.NotableErrorController.answersAreInvalid()),
            url => {
              println(s"url is!! $url")
              Redirect(Call(GET, url))
            }
          )
        }
      )
  }

//  private def upsertAndRedirect()
//  private def navigate(registration: Registration, url: String, mode: Mode): Call =
//    registration.contactAddressIsUk match {
//      case Some(ukMode) => Call(GET, url)
//      case _            => routes.NotableErrorController.answersAreInvalid()
//    }
}

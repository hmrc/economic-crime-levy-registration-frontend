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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.ConfirmContactAddressDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts.ContactsUtils
import uk.gov.hmrc.economiccrimelevyregistration.forms.ConfirmContactAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{ConfirmContactAddressPageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.AddressViewModel
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ConfirmContactAddressView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmContactAddressController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: ConfirmContactAddressFormProvider,
  pageNavigator: ConfirmContactAddressPageNavigator,
  dataCleanup: ConfirmContactAddressDataCleanup,
  view: ConfirmContactAddressView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController
    with ContactsUtils {

  val form: Form[Boolean]                        = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    (for {
      address <- request.contactAddress.asResponseError
    } yield address).fold(
      error => routeError(error),
      address =>
        Ok(
          view(
            form.prepare(request.registration.useRegisteredOfficeAddressAsContactAddress),
            AddressViewModel.insetText(address),
            mode
          )
        )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          (for {
            address <- request.contactAddress.asResponseError
          } yield address)
            .fold(
              error => Future.successful(routeError(error)),
              address => Future.successful(BadRequest(view(formWithErrors, AddressViewModel.insetText(address), mode)))
            ),
        useRegisteredOfficeAddressAsContactAddress => {
          val updatedRegistration = request.registration
            .copy(useRegisteredOfficeAddressAsContactAddress = Some(useRegisteredOfficeAddressAsContactAddress))

          val cleanedUpRegistration = dataCleanup.cleanup(updatedRegistration)

          val modifiedRegistration =
            if (cleanedUpRegistration.useRegisteredOfficeAddressAsContactAddress.contains(true)) {
              cleanedUpRegistration.copy(
                contactAddress = cleanedUpRegistration.grsAddressToEclAddress
              )
            } else {
              cleanedUpRegistration
            }

          (for {
            upsertedRegistration <- eclRegistrationService.upsertRegistration(modifiedRegistration).asResponseError
          } yield NavigationData(
            registration = upsertedRegistration
          )).convertToResult(mode, pageNavigator)
        }
      )
  }
}

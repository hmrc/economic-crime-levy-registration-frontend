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
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.ConfirmContactAddressPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.AddressViewModel
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AnswersAreInvalidView, ConfirmContactAddressView}
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
  answersAreInvalidView: AnswersAreInvalidView,
  view: ConfirmContactAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController
    with ContactsUtils {

  val form: Form[Boolean] = formProvider()

//  val address: RegistrationDataRequest[_] => EclAddress = r =>
//    r.registration.grsAddressToEclAddress.getOrElse(
//      throw new IllegalStateException("No registered office address found in registration data")
//    )



  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    (for {
      address <- request.contactAddress.asTestResponseError
    } yield address).fold(
      _ => Ok(answersAreInvalidView()),
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
            address <- request.contactAddress.asTestResponseError
          } yield address)
            .fold(
              _ => Future.successful(Ok(answersAreInvalidView())),
              address =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      AddressViewModel.insetText(address),
                      mode
                    )
                  )
                )
          ),
        useRegisteredOfficeAddressAsContactAddress => {
          val updatedRegistration = dataCleanup.cleanup(request.registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(useRegisteredOfficeAddressAsContactAddress)))

          (for {
            upsertedRegistration <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
          } yield upsertedRegistration).convertToAsyncResult(mode, pageNavigator)
        }


//        val updatedRegistration = dataCleanup.cleanup(request.registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(useRegisteredOfficeAddressAsContactAddress)))

//        useRegisteredOfficeAddressAsContactAddress =>
//          eclRegistrationConnector
//            .upsertRegistration(
//              dataCleanup.cleanup(
//                request.registration
//                  .copy(useRegisteredOfficeAddressAsContactAddress = Some(useRegisteredOfficeAddressAsContactAddress))
//              )
//            )
//            .flatMap { updatedRegistration =>
//              pageNavigator.nextPage(mode, updatedRegistration).map(Redirect)
//            }
      )
  }
}

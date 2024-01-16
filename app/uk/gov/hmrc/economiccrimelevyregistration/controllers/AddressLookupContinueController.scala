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

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{AddressLookupContinuePageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.{AddressLookupContinueService, EclRegistrationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AddressLookupContinueController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  addressLookupFrontendService: AddressLookupContinueService,
  eclRegistrationService: EclRegistrationService,
  pageNavigator: AddressLookupContinuePageNavigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler {

  def continue(mode: Mode, id: String): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>
      (for {
        address              <- addressLookupFrontendService.getAddress(id).asResponseError
        registration          = request.registration.copy(contactAddress = alfAddressToEclAddress(address))
        upsertedRegistration <- eclRegistrationService.upsertRegistration(registration).asResponseError
      } yield NavigationData(upsertedRegistration)).convertToResult(mode, pageNavigator)
  }

  private def alfAddressToEclAddress(alfAddressData: AlfAddressData): Option[EclAddress] =
    Some(
      EclAddress(
        organisation = alfAddressData.address.organisation,
        addressLine1 = alfAddressData.address.lines.headOption,
        addressLine2 = alfAddressData.address.lines.lift(1),
        addressLine3 = alfAddressData.address.lines.lift(2),
        addressLine4 = alfAddressData.address.lines.lift(3),
        region = None,
        postCode = alfAddressData.address.postcode,
        poBox = alfAddressData.address.poBox,
        countryCode = alfAddressData.address.country.code
      )
    )
}

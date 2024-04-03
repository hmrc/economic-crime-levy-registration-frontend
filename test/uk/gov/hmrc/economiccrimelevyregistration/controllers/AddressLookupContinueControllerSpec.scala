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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AddressLookupContinuePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{AddressLookupService, EclRegistrationService}

import scala.concurrent.Future

class AddressLookupContinueControllerSpec extends SpecBase {
  val mockAddressLookupFrontendService: AddressLookupService = mock[AddressLookupService]
  val mockEclRegistrationService: EclRegistrationService     = mock[EclRegistrationService]

  val pageNavigator: AddressLookupContinuePageNavigator = new AddressLookupContinuePageNavigator {
    override protected def navigateInNormalMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new AddressLookupContinueController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockAddressLookupFrontendService,
      mockEclRegistrationService,
      pageNavigator
    )
  }

  "continue" should {
    "retrieve the address data, store it then redirect to the check your answers page" in forAll {
      (journeyId: String, alfAddressData: AlfAddressData, registration: Registration) =>
        new TestContext(registration) {
          when(mockAddressLookupFrontendService.getAddress(ArgumentMatchers.eq(journeyId))(any()))
            .thenReturn(EitherT.fromEither[Future](Right(alfAddressData)))

          val updatedRegistration: Registration = registration.copy(contactAddress =
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
          )

          when(
            mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any())
          ).thenReturn(EitherT.fromEither[Future](Right(())))

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }
  }
}

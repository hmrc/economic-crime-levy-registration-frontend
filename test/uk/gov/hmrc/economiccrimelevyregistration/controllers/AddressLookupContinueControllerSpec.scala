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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{AddressLookupFrontendConnector, EclRegistrationConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData

import scala.concurrent.Future

class AddressLookupContinueControllerSpec extends SpecBase {
  val mockAddressLookupFrontendConnector: AddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]
  val mockEclRegistrationConnector: EclRegistrationConnector             = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new AddressLookupContinueController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck,
      fakeDataRetrievalAction(registrationData),
      mockAddressLookupFrontendConnector,
      mockEclRegistrationConnector
    )
  }

  "continue" should {
    "retrieve the address data, store it then redirect to the check your answers page" in forAll {
      (journeyId: String, alfAddressData: AlfAddressData, registration: Registration) =>
        new TestContext(registration) {
          when(
            mockAddressLookupFrontendConnector.getAddress(ArgumentMatchers.eq(journeyId))(any())
          ).thenReturn(Future.successful(alfAddressData))

          val updatedRegistration = registration.copy(contactAddress =
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
            mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any())
          ).thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] = controller.continue(NormalMode, journeyId)(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
        }
    }
  }
}

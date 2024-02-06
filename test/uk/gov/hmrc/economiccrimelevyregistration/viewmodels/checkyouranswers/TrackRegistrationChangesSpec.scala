/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkyouranswers

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, GetSubscriptionResponse, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers.TrackRegistrationChanges
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.BusinessSector.{InsolvencyPractitioner, TaxAdviser}

class TrackRegistrationChangesSpec extends SpecBase {

  def defaultEclRegistration(registration: Registration): Registration =
    registration.copy(registrationType = Some(Amendment))

  "isAmendRegistration" should {
    "return true when registrationType value is set to Amendment" in forAll {
      (
        registration: Registration
      ) =>
        val sut = TestTrackEclReturnChanges(
          defaultEclRegistration(registration),
          None
        )

        sut.isAmendRegistration shouldBe true
    }

    "return false when registrationType value is not set" in forAll {
      (
        registration: Registration
      ) =>
        val initialRegistration = defaultEclRegistration(registration).copy(registrationType = None)
        val sut                 = TestTrackEclReturnChanges(
          initialRegistration,
          None
        )

        sut.isAmendRegistration shouldBe false
    }
  }

  "isInitialRegistration" should {
    "return true when registrationType value is set to Initial" in forAll {
      (
        registration: Registration
      ) =>
        val initialRegistration = defaultEclRegistration(registration).copy(registrationType = Some(Initial))
        val sut                 = TestTrackEclReturnChanges(
          initialRegistration,
          None
        )

        sut.isInitialRegistration shouldBe true
    }

    "return false when registrationType value is not set" in forAll {
      (
        registration: Registration
      ) =>
        val initialRegistration = defaultEclRegistration(registration).copy(registrationType = None)
        val sut                 = TestTrackEclReturnChanges(
          initialRegistration,
          None
        )

        sut.isAmendRegistration shouldBe false
    }
  }

  "getFullName" should {
    "return full name when first name and last name are provided" in forAll {
      (
        registration: Registration
      ) =>
        val initialRegistration = defaultEclRegistration(registration).copy(registrationType = Some(Initial))
        val sut                 = TestTrackEclReturnChanges(
          initialRegistration,
          None
        )

        sut.getFullName("John", "Smith") shouldBe "John Smith"
    }
  }

  "hasBusinessSectorChanged" should {
    "return false if getSubscriptionResponse is missing due to journey being initial journey or getSubscriptionEnabled flag being set to false" in forAll {
      (
        registration: Registration
      ) =>
        val sut = TestTrackEclReturnChanges(
          defaultEclRegistration(registration),
          None
        )

        sut.hasBusinessSectorChanged shouldBe false
    }

    "return false if getSubscriptionResponse is present but business sector has not changed" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val validRegistration = registration.copy(businessSector = Some(InsolvencyPractitioner))

        val validResponse: GetSubscriptionResponse =
          response.copy(additionalDetails = response.additionalDetails.copy(businessSector = "InsolvencyPractitioner"))

        val sut = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasBusinessSectorChanged shouldBe false
    }

    "return true if getSubscriptionResponse is present and business sector has  changed" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val validRegistration = registration.copy(businessSector = Some(TaxAdviser))

        val validResponse: GetSubscriptionResponse =
          response.copy(additionalDetails = response.additionalDetails.copy(businessSector = "InsolvencyPractitioner"))

        val sut = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasBusinessSectorChanged shouldBe true
    }

    "return false if getSubscriptionResponse is present but business sector is not present" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val validRegistration = registration.copy(businessSector = None)

        val validResponse: GetSubscriptionResponse =
          response.copy(additionalDetails = response.additionalDetails.copy(businessSector = "InsolvencyPractitioner"))

        val sut = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasBusinessSectorChanged shouldBe false
    }
  }

  "hasAddressLine1Changed" should {
    "return false if getSubscriptionResponse is not present" in forAll { (registration: Registration) =>
      val sut = TestTrackEclReturnChanges(
        defaultEclRegistration(registration),
        None
      )

      sut.hasAddressLine1Changed shouldBe false
      sut.hasAddressChanged      shouldBe false
    }
    "return false if contactAddress is not present" in forAll {
      (response: GetSubscriptionResponse, registration: Registration) =>
        val invalidRegistration = registration.copy(contactAddress = None)
        val sut                 = TestTrackEclReturnChanges(
          defaultEclRegistration(invalidRegistration),
          Some(response)
        )

        sut.hasAddressLine1Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }

    "return false if getSubscriptionResponse is present but values are the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = Some(address),
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = None,
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine1 = address)
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine1Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }
    "return true if getSubscriptionResponse is present but values are not the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = Some(address),
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = None,
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine1 = "Valid address line")
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine1Changed shouldBe true
        sut.hasAddressChanged      shouldBe true
    }
  }

  "hasAddressLine2Changed" should {
    "return false if getSubscriptionResponse is not present" in forAll { (registration: Registration) =>
      val sut = TestTrackEclReturnChanges(
        defaultEclRegistration(registration),
        None
      )

      sut.hasAddressLine2Changed shouldBe false
      sut.hasAddressChanged      shouldBe false
    }
    "return false if contactAddress is not present" in forAll {
      (response: GetSubscriptionResponse, registration: Registration) =>
        val invalidRegistration = registration.copy(contactAddress = None)
        val invalidResponse     =
          response.copy(correspondenceAddressDetails = response.correspondenceAddressDetails.copy(addressLine2 = None))
        val sut                 = TestTrackEclReturnChanges(
          defaultEclRegistration(invalidRegistration),
          Some(invalidResponse)
        )

        sut.hasAddressLine2Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }

    "return false if getSubscriptionResponse is present but values are the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = Some(address),
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine2 = Some(address))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine2Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }
    "return true if getSubscriptionResponse is present but values are not the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = Some(address),
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine2 = Some("Valid address line"))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine2Changed shouldBe true
        sut.hasAddressChanged      shouldBe true
    }
  }

  "hasAddressLine3Changed" should {
    "return false if getSubscriptionResponse is not present" in forAll { (registration: Registration) =>
      val sut = TestTrackEclReturnChanges(
        defaultEclRegistration(registration),
        None
      )

      sut.hasAddressLine3Changed shouldBe false
      sut.hasAddressChanged      shouldBe false
    }
    "return false if contactAddress is not present" in forAll {
      (response: GetSubscriptionResponse, registration: Registration) =>
        val invalidRegistration = registration.copy(contactAddress = None)
        val invalidResponse     =
          response.copy(correspondenceAddressDetails = response.correspondenceAddressDetails.copy(addressLine3 = None))
        val sut                 = TestTrackEclReturnChanges(
          defaultEclRegistration(invalidRegistration),
          Some(invalidResponse)
        )

        sut.hasAddressLine3Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }

    "return false if getSubscriptionResponse is present but values are the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = Some(address),
                addressLine2 = None,
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine3 = Some(address))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine3Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }
    "return true if getSubscriptionResponse is present but values are not the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = Some(address),
                addressLine2 = None,
                addressLine4 = None,
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine3 = Some("Valid address line"))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine3Changed shouldBe true
        sut.hasAddressChanged      shouldBe true
    }
  }

  "hasAddressLine4Changed" should {
    "return false if getSubscriptionResponse is not present" in forAll { (registration: Registration) =>
      val sut = TestTrackEclReturnChanges(
        defaultEclRegistration(registration),
        None
      )

      sut.hasAddressLine4Changed shouldBe false
      sut.hasAddressChanged      shouldBe false
    }
    "return false if contactAddress is not present" in forAll {
      (response: GetSubscriptionResponse, registration: Registration) =>
        val invalidRegistration = registration.copy(contactAddress = None)
        val invalidResponse     =
          response.copy(correspondenceAddressDetails = response.correspondenceAddressDetails.copy(addressLine4 = None))
        val sut                 = TestTrackEclReturnChanges(
          defaultEclRegistration(invalidRegistration),
          Some(invalidResponse)
        )

        sut.hasAddressLine4Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }

    "return false if getSubscriptionResponse is present but values are the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = None,
                addressLine4 = Some(address),
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine4 = Some(address))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine4Changed shouldBe false
        sut.hasAddressChanged      shouldBe false
    }
    "return true if getSubscriptionResponse is present but values are not the same" in forAll {
      (registration: Registration, response: GetSubscriptionResponse) =>
        val address           = "Address line"
        val validRegistration =
          registration.copy(contactAddress =
            Some(
              EclAddress(
                addressLine1 = None,
                countryCode = "GB",
                organisation = None,
                addressLine3 = None,
                addressLine2 = None,
                addressLine4 = Some(address),
                region = None,
                postCode = None,
                poBox = None
              )
            )
          )

        val validResponse =
          response.copy(correspondenceAddressDetails =
            response.correspondenceAddressDetails.copy(addressLine4 = Some("Valid address line"))
          )
        val sut           = TestTrackEclReturnChanges(
          defaultEclRegistration(validRegistration),
          Some(validResponse)
        )

        sut.hasAddressLine4Changed shouldBe true
        sut.hasAddressChanged      shouldBe true
    }
  }
}

final case class TestTrackEclReturnChanges(
  registration: Registration,
  getSubscriptionResponse: Option[GetSubscriptionResponse]
) extends TrackRegistrationChanges

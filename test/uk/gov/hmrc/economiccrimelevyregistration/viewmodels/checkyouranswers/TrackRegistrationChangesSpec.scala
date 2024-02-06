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
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
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
}

final case class TestTrackEclReturnChanges(
  registration: Registration,
  getSubscriptionResponse: Option[GetSubscriptionResponse]
) extends TrackRegistrationChanges

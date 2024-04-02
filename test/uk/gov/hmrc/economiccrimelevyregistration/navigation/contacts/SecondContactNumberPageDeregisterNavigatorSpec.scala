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

package uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts

import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode, Registration}

class SecondContactNumberPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new SecondContactNumberPageNavigator()

  "nextPage" should {
    "return a Call to the confirm contact address page in NormalMode when there is a valid address present in the GRS journey data" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile,
        number: String
      ) =>
        val updatedRegistration: Registration =
          registration.copy(
            contacts = registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(telephoneNumber = Some(number))
            ),
            incorporatedEntityJourneyData =
              Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
            partnershipEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.ConfirmContactAddressController.onPageLoad(NormalMode)
    }

    "return a Call to the contact address in the UK page in NormalMode when there is no valid address present in the GRS journey data" in forAll {
      (
        registration: Registration,
        number: String
      ) =>
        val updatedRegistration: Registration =
          registration.copy(
            contacts = registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(telephoneNumber = Some(number))
            ),
            incorporatedEntityJourneyData = None,
            partnershipEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.IsUkAddressController.onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode" in forAll { registration: Registration =>
      val updatedRegistration: Registration =
        registration.copy(
          contacts = registration.contacts.copy(secondContactDetails = validContactDetails)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial))
    }
  }

}

/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class IsUkAddressPageNavigatorSpec extends SpecBase {

  val pageNavigator = new IsUkAddressPageNavigator()

  "previousPage" should {
    "return a call to the second contact number page when the answer was yes to adding another contact and there is no valid address in the GRS journey data" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(
            contacts = registration.contacts.copy(secondContact = Some(true)),
            incorporatedEntityJourneyData = None,
            partnershipEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

        pageNavigator.previousPage(updatedRegistration) shouldBe contacts.routes.SecondContactNumberController
          .onPageLoad()
    }

    "return a call to the add another contact page when the answer was no adding another contact and there is no valid address in the GRS journey data" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(
            contacts = registration.contacts.copy(secondContact = Some(false)),
            incorporatedEntityJourneyData = None,
            partnershipEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

        pageNavigator.previousPage(updatedRegistration) shouldBe contacts.routes.AddAnotherContactController
          .onPageLoad()
    }

    "return a call to the confirm contact address page when there is a valid address in the GRS journey data" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile
      ) =>
        val updatedRegistration: Registration =
          registration.copy(
            incorporatedEntityJourneyData =
              Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
            partnershipEntityJourneyData = None,
            soleTraderEntityJourneyData = None
          )

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.ConfirmContactAddressController
          .onPageLoad()
    }
  }

}

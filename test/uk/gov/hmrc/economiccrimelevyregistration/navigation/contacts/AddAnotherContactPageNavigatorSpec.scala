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

package uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class AddAnotherContactPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AddAnotherContactPageNavigator

  "nextPage" should {
    "return a Call to second contact name page in NormalMode when the 'Yes' option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = Some(true)))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe contacts.routes.SecondContactNameController
          .onPageLoad()
    }

    "return a Call to the confirm contact address page in NormalMode when the 'No' option is selected and there is a valid address present in the GRS journey data" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile
      ) =>
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContact = Some(false)),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.ConfirmContactAddressController
          .onPageLoad()
    }

    "return a Call to the contact address in the UK page in NormalMode when the 'No' option is selected and there is no valid address present in the GRS journey data" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContact = Some(false)),
          incorporatedEntityJourneyData = None,
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.IsUkAddressController
          .onPageLoad()
    }
  }

}

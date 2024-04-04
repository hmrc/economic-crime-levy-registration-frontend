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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, ContactDetails, EclRegistrationModel, NormalMode, Registration}

class AddAnotherContactPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AddAnotherContactPageNavigator

  "navigateInNormalMode" should {
    "return a Call to the second contact name page in NormalMode when the 'Yes' option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = Some(true)))

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          contacts.routes.SecondContactNameController.onPageLoad(NormalMode)
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

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.ConfirmContactAddressController.onPageLoad(NormalMode)
    }

    "return a Call to the contact address in the UK page in NormalMode when the 'No' option is selected and there is no valid address present in the GRS journey data" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContact = Some(false)),
          incorporatedEntityJourneyData = None,
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        pageNavigator.nextPage(
          NormalMode,
          EclRegistrationModel(updatedRegistration)
        ) shouldBe routes.IsUkAddressController
          .onPageLoad(NormalMode)
    }

    "return a call to the answers are invalid page when the secondContact is set to None" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = None))

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.NotableErrorController.answersAreInvalid()
    }
  }

  "navigateInCheckMode" should {
    "return a Call to the second contact name page when the 'Yes' option is selected and there are no second contact details already present" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(secondContact = Some(true), secondContactDetails = ContactDetails.empty)
        )

        pageNavigator.nextPage(
          CheckMode,
          EclRegistrationModel(updatedRegistration)
        ) shouldBe contacts.routes.SecondContactNameController
          .onPageLoad(CheckMode)
    }

    "return a Call to the check your answers page when the 'Yes' option is selected and there is a second contact name already present" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(
            secondContact = Some(true),
            secondContactDetails = validContactDetails
          )
        )

        pageNavigator.nextPage(
          CheckMode,
          EclRegistrationModel(updatedRegistration)
        ) shouldBe routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial))
    }

    "return a Call to the check your answers page when the 'No' option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts =
          registration.contacts.copy(secondContact = Some(false), secondContactDetails = ContactDetails.empty)
        )

        pageNavigator.nextPage(
          CheckMode,
          EclRegistrationModel(updatedRegistration)
        ) shouldBe routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial))
    }

    "return a call to the answers are invalid page when the secondContact is set to None" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(contacts = registration.contacts.copy(secondContact = None))

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.NotableErrorController.answersAreInvalid()
    }
  }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class ConfirmContactAddressPageNavigatorSpec extends SpecBase {

  val pageNavigator = new ConfirmContactAddressPageNavigator()

  "nextPage" should {
    "return a Call to the UK address question page in NormalMode when the answer is no" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false))

        pending
    }

    "return a Call to the check your answers page in NormalMode when the answer is yes" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(true))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }
  }

  "previousPage" should {
    "return a call to the second contact number page when the answer was yes to adding another contact" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(contacts = registration.contacts.copy(secondContact = Some(true)))

        pageNavigator.previousPage(updatedRegistration) shouldBe contacts.routes.SecondContactNumberController
          .onPageLoad()
    }

    "return a call to the add another contact page when the answer was no adding another contact" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(contacts = registration.contacts.copy(secondContact = Some(false)))

        pageNavigator.previousPage(updatedRegistration) shouldBe contacts.routes.AddAnotherContactController
          .onPageLoad()
    }
  }

}

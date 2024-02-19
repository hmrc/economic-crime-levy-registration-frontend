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

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, NormalMode, Registration}

class SecondContactEmailPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new SecondContactEmailPageNavigator()

  "nextPage" should {
    "return a Call to the second contact telephone number page in NormalMode" in forAll {
      (registration: Registration, email: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(emailAddress = Some(email))
            )
          )

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
          contacts.routes.SecondContactNumberController.onPageLoad(NormalMode)
    }

    "return a Call to the second contact telephone number page in CheckMode when a second contact telephone number does not already exist" in forAll {
      (registration: Registration, email: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(emailAddress = Some(email), telephoneNumber = None)
            )
          )

        pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
          contacts.routes.SecondContactNumberController.onPageLoad(CheckMode)
    }

    "return a Call to the check your answers page in CheckMode when a second contact telephone number already exists" in forAll {
      (registration: Registration, email: String, telephoneNumber: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails
                .copy(emailAddress = Some(email), telephoneNumber = Some(telephoneNumber))
            )
          )

        pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

}

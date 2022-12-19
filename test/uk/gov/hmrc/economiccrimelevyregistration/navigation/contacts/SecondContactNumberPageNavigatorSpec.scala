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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class SecondContactNumberPageNavigatorSpec extends SpecBase {

  val pageNavigator = new SecondContactNumberPageNavigator()

  "nextPage" should {
    "return a Call to the registered office address page in NormalMode" in forAll {
      (registration: Registration, number: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(telephoneNumber = Some(number))
            )
          )

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.ConfirmContactAddressController
          .onPageLoad()
    }
  }

}

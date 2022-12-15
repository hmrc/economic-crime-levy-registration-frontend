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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class FirstContactNamePageNavigatorSpec extends SpecBase {

  val pageNavigator = new FirstContactNamePageNavigator()

  "nextPage" should {
    "return a Call to the first contact role page in NormalMode" in forAll {
      (registration: Registration, name: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(firstContactDetails =
              registration.contacts.firstContactDetails.copy(name = Some(name))
            )
          )

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.FirstContactRoleController.onPageLoad()
    }
  }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode, Registration}

class SecondContactRolePageNavigatorSpec extends SpecBase {

  val pageNavigator = new SecondContactRolePageNavigator()

  "nextPage" should {
    "return a Call to the second contact email page in NormalMode" in forAll {
      (registration: Registration, role: String) =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails =
              registration.contacts.secondContactDetails.copy(role = Some(role))
            )
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          contacts.routes.SecondContactEmailController.onPageLoad(NormalMode)
    }

    "return a Call to the second contact email page in CheckMode when a second contact email is not already present" in forAll {
      registration: Registration =>
        val updatedRegistration: Registration =
          registration.copy(contacts =
            registration.contacts.copy(secondContactDetails = validContactDetails.copy(emailAddress = None))
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          contacts.routes.SecondContactEmailController.onPageLoad(CheckMode)
    }

    "return a Call to the check your answers page in CheckMode when second contact details are already present" in forAll {
      registration: Registration =>
        val updatedRegistration: Registration =
          registration.copy(contacts = registration.contacts.copy(secondContactDetails = validContactDetails))

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    Seq(NormalMode, CheckMode).foreach { mode =>
      s"return a call to the answers are invalid page when there is no second contact role present in $mode " in forAll {
        registration: Registration =>
          val updatedRegistration: Registration =
            registration.copy(contacts =
              registration.contacts.copy(secondContactDetails =
                registration.contacts.secondContactDetails.copy(role = None)
              )
            )

          pageNavigator.nextPage(mode, EclRegistrationModel(updatedRegistration)) shouldBe
            routes.NotableErrorController.answersAreInvalid()
      }
    }

  }

}

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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{BusinessSector, NormalMode, Registration}

class CheckYourAnswersPageNavigatorSpec extends SpecBase {

  val pageNavigator = new CheckYourAnswersPageNavigator()

  "previousPage" should {
    "return a call to the confirm contact address page when the answer was yes to using the registered office address" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(true))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.ConfirmContactAddressController
          .onPageLoad()
    }

    "return a call to is address in uk page when the answer was no to using the registered office address" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.IsUkAddressController.onPageLoad()
    }
  }

}

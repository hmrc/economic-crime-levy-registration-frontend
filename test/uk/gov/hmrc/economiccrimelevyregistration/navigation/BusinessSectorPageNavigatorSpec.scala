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

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.{BusinessSector, NormalMode, Registration}

class BusinessSectorPageNavigatorSpec extends SpecBase {

  val pageNavigator = new BusinessSectorPageNavigator()

  "nextPage" should {
    "return a Call to the contact name page in NormalMode" in forAll {
      (registration: Registration, businessSector: BusinessSector) =>
        val updatedRegistration: Registration = registration.copy(businessSector = Some(businessSector))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe contacts.routes.FirstContactNameController
          .onPageLoad()
    }
  }

  "previousPage" should {
    "return a call to the aml regulated start date page when the answer was yes to becoming regulated in the current FY" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration = registration.copy(startedAmlRegulatedActivityInCurrentFy = Some(true))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.AmlRegulatedActivityStartDateController
          .onPageLoad()
    }

    "return a call to the aml regulated page when the answer was no to becoming regulated in the current FY" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration = registration.copy(startedAmlRegulatedActivityInCurrentFy = Some(false))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.AmlRegulatedController.onPageLoad()
    }
  }

}

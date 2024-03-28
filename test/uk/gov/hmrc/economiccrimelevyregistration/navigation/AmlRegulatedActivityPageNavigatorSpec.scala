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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, Mode, NormalMode, Registration}

class AmlRegulatedActivityPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlRegulatedActivityPageNavigator()

  "nextPage" should {
    "return a Call to the AML supervisor page from the AML regulated activity page in NormalMode when the 'Yes' option is selected" in forAll {
      (registration: Registration, mode: Mode) =>
        val updatedRegistration =
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true), registrationType = Some(Amendment))

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(registration = updatedRegistration, hasRegistrationChanged = true)
        ) shouldBe
          routes.AmlSupervisorController.onPageLoad(mode, Amendment)
    }

    "return a Call to the check your answers page from the AML regulated activity page in CheckMode when the 'Yes' option is selected" in forAll {
      (registration: Registration, mode: Mode) =>
        val updatedRegistration =
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true), registrationType = Some(Initial))

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(registration = updatedRegistration, hasRegistrationChanged = true)
        ) shouldBe
          routes.AmlSupervisorController.onPageLoad(mode, Initial)
    }

    "return a Call to the liable in previous year page from the AML regulated activity page in either mode when the 'No' option is selected" in forAll {
      (registration: Registration, mode: Mode) =>
        val updatedRegistration =
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false), registrationType = Some(Initial))

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(registration = updatedRegistration, hasRegistrationChanged = true)
        ) shouldBe
          routes.LiabilityBeforeCurrentYearController.onPageLoad(mode)
    }

    "return a Call to the check your answers page from the AML regulated activity page if data hasn't changed" in forAll {
      registration: Registration =>
        pageNavigator.nextPage(
          CheckMode,
          EclRegistrationModel(registration = registration, hasRegistrationChanged = false)
        ) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

}

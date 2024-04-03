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

import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode, Registration}

class ConfirmContactAddressPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new ConfirmContactAddressPageNavigator()

  "nextPage" should {
    "return a Call to the UK address question page in either mode when the answer is no" in forAll {
      (registration: Registration, mode: Mode) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false))

        pageNavigator.nextPage(mode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.IsUkAddressController.onPageLoad(mode)
    }

    "return a Call to the check your answers page in either mode when the answer is yes" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile,
        mode: Mode
      ) =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(true),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        pageNavigator.nextPage(mode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial))
    }
  }

}

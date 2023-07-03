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

import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.{RegistrationWithUnincorporatedAssociation, ValidTrustRegistration}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode

class CtUtrPageNavigatorSpec extends SpecBase {

  val pageNavigator = new CtUtrPageNavigator()

  "nextPage" should {
    "redirect to Other entity check your answer page when Trust registration is valid in NormalMode" in {
      (validTrustRegistration: ValidTrustRegistration) =>
        pageNavigator.nextPage(
          NormalMode,
          validTrustRegistration.registration
        ) shouldBe routes.OtherEntityCheckYourAnswersController.onPageLoad()
    }

    "redirect to postcode page when Unincorporated Association registration is valid in NormalMode" in {
      (registrationWithUnincorporatedAssociation: RegistrationWithUnincorporatedAssociation) =>
        pageNavigator.nextPage(
          NormalMode,
          registrationWithUnincorporatedAssociation.registration
        ) shouldBe routes.CtUtrPostcodeController.onPageLoad(NormalMode)
    }
  }

}

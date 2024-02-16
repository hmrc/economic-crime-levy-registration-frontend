/*
 * Copyright 2024 HM Revenue & Customs
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

import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment

class AmendReasonPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new AmendReasonPageNavigator()

  "nextPage" should {
    "return a call to AML supervisor in NormalMode" in forAll(
      Arbitrary.arbitrary[Registration],
      nonEmptyString
    ) { (registration: Registration, reason: String) =>
      val updatedRegistration = registration.copy(amendReason = Some(reason), registrationType = Some(Amendment))

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.AmlSupervisorController
        .onPageLoad(NormalMode, Amendment)
    }
    "return a Call to the check your answers page in CheckMode" in forAll(
      Arbitrary.arbitrary[Registration],
      nonEmptyString
    ) { (registration: Registration, reason: String) =>
      val updatedRegistration: Registration = registration.copy(amendReason = Some(reason))

      pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }
  }
}

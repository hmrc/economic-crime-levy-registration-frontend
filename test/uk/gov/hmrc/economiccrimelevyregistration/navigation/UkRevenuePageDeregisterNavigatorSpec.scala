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
import uk.gov.hmrc.economiccrimelevyregistration.models._

class UkRevenuePageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new UkRevenuePageNavigator()

  "nextPage" should {
    "return a Call to the liable before current year in NormalMode when the revenue meets threshold flag is true" in forAll {
      (registration: Registration, ukRevenue: Long) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(true))

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode when the revenue meets threshold flag is true" in forAll {
      (registration: Registration, ukRevenue: Long) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(true))

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    "return a Call to the liable in previous year page in either mode when the revenue meets threshold flag is false" in forAll {
      (registration: Registration, ukRevenue: Long, mode: Mode) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(false))

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(updatedRegistration)
        ) shouldBe routes.LiabilityBeforeCurrentYearController
          .onPageLoad(mode)
    }
  }

}

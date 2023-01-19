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
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class NotLiablePageNavigatorSpec extends SpecBase {

  val pageNavigator = new NotLiablePageNavigator()

  "previousPage" should {
    "return a call to the AML regulated activity page when no AML regulated activity was carried out in FY" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.AmlRegulatedActivityController
          .onPageLoad()
    }

    "return a call to the UK revenue page when AML regulated activity was carried out in FY" in forAll {
      (registration: Registration, revenue: Long) =>
        val updatedRegistration: Registration =
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true), relevantApRevenue = Some(revenue))

        pageNavigator.previousPage(updatedRegistration) shouldBe routes.UkRevenueController.onPageLoad()
    }
  }

}

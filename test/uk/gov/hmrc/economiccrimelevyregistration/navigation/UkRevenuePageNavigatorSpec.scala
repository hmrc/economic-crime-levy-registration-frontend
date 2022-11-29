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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class UkRevenuePageNavigatorSpec extends SpecBase {

  val pageNavigator = new UkRevenuePageNavigator

  "nextPage" should {
    "go to the AML Supervisor page from the UK revenue page in NormalMode when the entity meets the revenue threshold" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(true))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.AmlSupervisorController
          .onPageLoad()
    }

    "go to the not liable page from the UK revenue page in NormalMode when the entity does not meet the revenue threshold" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(false))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.NotLiableController
          .onPageLoad()
    }
  }

}

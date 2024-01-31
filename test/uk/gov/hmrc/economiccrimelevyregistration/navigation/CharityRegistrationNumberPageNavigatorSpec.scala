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

import org.scalacheck.Arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CharityRegistrationNumberPageNavigatorSpec extends SpecBase {

  val pageNavigator = new CharityRegistrationNumberPageNavigator()

  "nextPage" should {
    "(Normal Mode) return a call to the utr question page" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsLongerThan(1)
    ) { (registration: Registration, charityRegistrationNumber: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          charityRegistrationNumber = Some(charityRegistrationNumber)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(Charity),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
        routes.DoYouHaveUtrController.onPageLoad(NormalMode)
    }

    "(Check Mode) return a call to the check your answers page" in forAll {
      (registration: Registration, number: String) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            companyRegistrationNumber = Some(number)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._

class DoYouHaveUtrPageNavigatorSpec extends SpecBase {

  val pageNavigator = new DoYouHaveUtrPageNavigator()

  "nextPage" should {
    "(Normal Mode) return a call to the utr page when answer is yes" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(true)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(Charity),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, NavigationData(updatedRegistration)) shouldBe
        routes.UtrController.onPageLoad(NormalMode)
    }

    "(Normal Mode) return a call to the company registration number page when answer is no" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(false)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(NormalMode, NavigationData(updatedRegistration)) shouldBe
          routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)
    }

    "(Check Mode) return a call to the check your answers page" in forAll {
      (registration: Registration, isUtrPresent: Boolean, utr: String) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(isUtrPresent),
            ctUtr = isUtrPresent match {
              case true  => Some(utr)
              case false => None
            }
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, NavigationData(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

}

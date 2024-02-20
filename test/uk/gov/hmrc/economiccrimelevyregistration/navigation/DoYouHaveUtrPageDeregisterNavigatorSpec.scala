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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class DoYouHaveUtrPageDeregisterNavigatorSpec extends SpecBase {

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
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      val nextPage = if (registration.entityType.contains(UnincorporatedAssociation)) {
        routes.UtrTypeController.onPageLoad(NormalMode)
      } else {
        routes.UtrController.onPageLoad(NormalMode)
      }

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
        nextPage
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
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        val nextPage = if (registration.entityType.contains(UnincorporatedAssociation)) {
          routes.BusinessSectorController.onPageLoad(NormalMode)
        } else {
          routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)
        }

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
          nextPage
    }

    "(Check Mode) return a call to the check your answers page if ctUtr is present" in forAll {
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

        pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }
  }

}

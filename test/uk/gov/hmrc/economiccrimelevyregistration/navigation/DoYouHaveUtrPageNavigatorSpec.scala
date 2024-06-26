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
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      val nextPage = if (updatedRegistration.entityType.contains(UnincorporatedAssociation)) {
        routes.UtrTypeController.onPageLoad(NormalMode)
      } else {
        routes.UtrController.onPageLoad(NormalMode)
      }

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage
    }

    "(Normal Mode) return a call to the correct page when answer is no" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(false)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      val nextPage = if (updatedRegistration.entityType.contains(UnincorporatedAssociation)) {
        routes.BusinessSectorController.onPageLoad(NormalMode)
      } else {
        routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)
      }

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage
    }

    "(Check Mode) return a call to the check your answers page if ctUtr is present and ctUtr contains a value" in forAll {
      (registration: Registration, isUtrPresent: Boolean, utr: String, number: String) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(isUtrPresent),
            ctUtr = if (isUtrPresent) {
              Some(utr)
            } else {
              None
            },
            companyRegistrationNumber = Some(number)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    "(Check Mode) return a call to the Utr type page if ctUtr is present and ctUtr does not contain a value" in forAll {
      (registration: Registration, number: String) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(true),
            ctUtr = None,
            companyRegistrationNumber = Some(number)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        val nextPage = if (updatedRegistration.entityType.contains(UnincorporatedAssociation)) {
          routes.UtrTypeController.onPageLoad(CheckMode)
        } else {
          routes.UtrController.onPageLoad(CheckMode)
        }

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          nextPage
    }

    "(Check Mode) return a call to the correct page when answer is no" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(false)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      val nextPage = if (updatedRegistration.entityType.contains(UnincorporatedAssociation)) {
        routes.CheckYourAnswersController.onPageLoad()
      } else {
        if (updatedRegistration.otherEntityJourneyData.companyRegistrationNumber.isEmpty) {
          routes.CompanyRegistrationNumberController.onPageLoad(CheckMode)
        } else { routes.CheckYourAnswersController.onPageLoad() }
      }

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage
    }

  }

}

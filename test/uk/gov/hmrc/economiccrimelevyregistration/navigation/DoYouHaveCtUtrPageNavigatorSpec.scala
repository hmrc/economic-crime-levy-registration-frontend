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

class DoYouHaveCtUtrPageNavigatorSpec extends SpecBase {

  val pageNavigator = new DoYouHaveCtUtrPageNavigator()

  "navigateInNormalMode" should {
    "return a call to the ctUtr page when the answer is yes" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(true)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CtUtrController.onPageLoad(NormalMode)

    }

    "return a call to the Business Sector page when the answer is no" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(false)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.BusinessSectorController.onPageLoad(NormalMode)

    }

    "return a call to the Answers are invalid page when there is no answer present" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.NotableErrorController.answersAreInvalid()

    }

  }

  "navigateInCheckMode" should {
    "return a call to the CheckYourAnswers page when the answer is yes and a postcode is present" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(true),
            postcode = Some("Postcode")
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()

    }

    "return a call to the CtUtr page when the answer is yes but no postcode is present" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(true),
            postcode = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CtUtrController.onPageLoad(CheckMode)

    }

    "return a call to the CheckYourAnswers page when the answer is no" in forAll { (registration: Registration) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(false)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CheckYourAnswersController.onPageLoad()

    }

    "return a call to the Answers are invalid page when there is no answer present" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.NotableErrorController.answersAreInvalid()
    }

  }
}

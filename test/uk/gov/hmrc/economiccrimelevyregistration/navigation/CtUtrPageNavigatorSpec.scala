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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{NonUKEstablishment, Trust}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CtUtrPageNavigatorSpec extends SpecBase {

  val pageNavigator = new CtUtrPageNavigator()

  "navigateInNormalMode" should {
    Seq(Trust, NonUKEstablishment).foreach { entityType =>
      s"return a call to the Business Sector page when a ctUtr is present and the entity type is $entityType" in forAll {
        (registration: Registration) =>
          val otherEntityJourneyData = OtherEntityJourneyData
            .empty()
            .copy(
              ctUtr = Some("utr")
            )

          val updatedRegistration: Registration =
            registration.copy(
              optOtherEntityJourneyData = Some(otherEntityJourneyData),
              entityType = Some(entityType)
            )

          pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
            routes.BusinessSectorController.onPageLoad(NormalMode)
      }
    }

    "return a call to the CtUtr Postcode page when a utr is present and the entity type is not Trust or NonUkEstablishment" in forAll(
      Arbitrary.arbitrary[Registration],
      entityTypeThatIsNot(Seq(Trust, NonUKEstablishment))
    ) { (registration: Registration, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          ctUtr = Some("utr")
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          entityType = Some(entityType)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CtUtrPostcodeController.onPageLoad(NormalMode)
    }

    "return a call to the answers are invalid page if entity type is None" in forAll { registration: Registration =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          ctUtr = Some("utr")
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          entityType = None
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.NotableErrorController.answersAreInvalid()
    }

    "return a call to the answers are invalid page if ctUtr is None" in forAll { registration: Registration =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          ctUtr = None
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          entityType = None
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.NotableErrorController.answersAreInvalid()
    }
  }

  "navigateInCheckMode" should {
    Seq(Trust, NonUKEstablishment).foreach { entityType =>
      s"return a call to the Business Sector page when a ctUtr is present and the entity type is $entityType" in forAll {
        (registration: Registration) =>
          val otherEntityJourneyData = OtherEntityJourneyData
            .empty()
            .copy(
              ctUtr = Some("utr")
            )

          val updatedRegistration: Registration =
            registration.copy(
              optOtherEntityJourneyData = Some(otherEntityJourneyData),
              entityType = Some(entityType)
            )

          pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
            routes.CheckYourAnswersController.onPageLoad()
      }
    }

    "return a call to the answers are invalid page when the entityType is set to None" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(
            entityType = None
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.NotableErrorController.answersAreInvalid()

    }

    "return a call to the answers are invalid page when the entityType is something other than Trust or NonUkEstablishment and isCtUtrPresent is set to None" in forAll(
      Arbitrary.arbitrary[Registration],
      entityTypeThatIsNot(Seq(Trust, NonUKEstablishment))
    ) { (registration: Registration, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = None
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          entityType = Some(entityType)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.NotableErrorController.answersAreInvalid()

    }

    "return a call to the CheckYourAnswers page when the entityType is something other than Trust or NonUkEstablishment and isCtUtrPresent is Some(false)" in forAll(
      Arbitrary.arbitrary[Registration],
      entityTypeThatIsNot(Seq(Trust, NonUKEstablishment))
    ) { (registration: Registration, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isCtUtrPresent = Some(false)
        )

      val updatedRegistration: Registration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          entityType = Some(entityType)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CheckYourAnswersController.onPageLoad()

    }

    "return a call to the CheckYourAnswers page when the entityType is something other than Trust or NonUkEstablishment" +
      " and isCtUtrPresent is Some(true) and the otherEntityJourneyData.postcode contains a value" in forAll(
        Arbitrary.arbitrary[Registration],
        entityTypeThatIsNot(Seq(Trust, NonUKEstablishment))
      ) { (registration: Registration, entityType: EntityType) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(true),
            postcode = Some("postcode")
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData),
            entityType = Some(entityType)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()

      }

    "return a call to the CheckYourAnswers page when the entityType is something other than Trust or NonUkEstablishment" +
      " and isCtUtrPresent is Some(true) and the otherEntityJourneyData.postcode is set to None" in forAll(
        Arbitrary.arbitrary[Registration],
        entityTypeThatIsNot(Seq(Trust, NonUKEstablishment))
      ) { (registration: Registration, entityType: EntityType) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(true),
            postcode = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData),
            entityType = Some(entityType)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CtUtrPostcodeController.onPageLoad(CheckMode)

      }

  }
}

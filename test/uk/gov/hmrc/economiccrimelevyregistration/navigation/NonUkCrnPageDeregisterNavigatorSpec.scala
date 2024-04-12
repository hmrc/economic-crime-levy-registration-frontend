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

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models._

class NonUkCrnPageDeregisterNavigatorSpec extends SpecBase {

  val pageNavigator = new NonUkCrnPageNavigator()

  "nextPage" should {
    "return a call to the UTR type page in Normal mode" in forAll { (registration: Registration) =>
      val nextPage = if (registration.entityType.contains(UnincorporatedAssociation)) {
        routes.DoYouHaveUtrController.onPageLoad(NormalMode)
      } else {
        routes.UtrTypeController.onPageLoad(NormalMode)
      }

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(registration)) shouldBe
        nextPage
    }

    "return a call to the UtrType page in Check mode when the entity type is not UnincorporatedAssociation and utrType is empty" in forAll(
      Arbitrary.arbitrary[Registration],
      Gen.oneOf(EntityType.values).suchThat(_ != UnincorporatedAssociation)
    ) { (registration: Registration, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          utrType = None
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(entityType),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.UtrTypeController.onPageLoad(CheckMode)
    }

    "return a call to the DoYouHaveUtr page in Check mode when the entity type is UnincorporatedAssociation and isCtUtrPresent is empty" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(UnincorporatedAssociation),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.DoYouHaveUtrController.onPageLoad(CheckMode)
    }

    "return a call to the check your answers page in Check mode when the entity type is UnincorporatedAssociation and contains isCtUtrPresent" in forAll {
      (registration: Registration, isCtUtrPresent: Boolean) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(isCtUtrPresent)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(UnincorporatedAssociation),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    "return a call to the check your answers page in Check mode when the entity type is not UnincorporatedAssociation and contains utrType" in forAll(
      Arbitrary.arbitrary[Registration],
      Arbitrary.arbitrary[UtrType],
      Gen.oneOf(EntityType.values).suchThat(_ != UnincorporatedAssociation)
    ) { (registration: Registration, utrType: UtrType, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          utrType = Some(utrType)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(entityType),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.CheckYourAnswersController.onPageLoad()
    }

  }

}

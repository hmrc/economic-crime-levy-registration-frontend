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
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.{CharityRegistrationNumberMaxLength, OrganisationNameMaxLength}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessNamePageNavigatorSpec extends SpecBase {

  val pageNavigator = new BusinessNamePageNavigator()

  val nextPage: Map[EntityType, Call] = Map(
    Charity                   -> routes.CharityRegistrationNumberController.onPageLoad(NormalMode),
    UnincorporatedAssociation -> routes.DoYouHaveCrnController.onPageLoad(NormalMode),
    Trust                     -> routes.CtUtrController.onPageLoad(NormalMode),
    NonUKEstablishment        -> routes.DoYouHaveCrnController.onPageLoad(NormalMode)
  )

  "nextPage" should {
    "(Normal Mode) return a call to the charity page if charity selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(OrganisationNameMaxLength),
      Gen.oneOf[EntityType](Charity, Trust, NonUKEstablishment, UnincorporatedAssociation)
    ) { (registration: Registration, businessName: String, entityType: EntityType) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(entityType),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
        nextPage(entityType)
    }

    "(Check Mode) return a call to the check your answers page if charity selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(OrganisationNameMaxLength),
      stringsWithMaxLength(CharityRegistrationNumberMaxLength)
    ) { (registration: Registration, businessName: String, number: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName),
          charityRegistrationNumber = Some(number)
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

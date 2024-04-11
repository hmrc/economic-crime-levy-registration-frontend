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
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.{charityRegistrationNumberMaxLength, companyRegistrationNumberMaxLength, organisationNameMaxLength, utrLength}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
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
    "(Normal Mode) return a call to the charity registration number page if charity selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) { (registration: Registration, businessName: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(Charity),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage(Charity)
    }

    "(Normal Mode) return a call to the Company registration number page if UnincorporatedAssociation is selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) { (registration: Registration, businessName: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(UnincorporatedAssociation),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage(UnincorporatedAssociation)
    }

    "(Normal Mode) return a call to the Company registration number page if NonUkEstablishment is selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) { (registration: Registration, businessName: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(NonUKEstablishment),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage(NonUKEstablishment)
    }

    "(Normal Mode) return a call to the CtUtr page if Trust is selected" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) { (registration: Registration, businessName: String) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName)
        )

      val updatedRegistration: Registration =
        registration.copy(
          entityType = Some(Trust),
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        nextPage(Trust)
    }

    "(Normal mode) return a call to the answers are invalid page if entity type is not present" in forAll(
      Arbitrary.arbitrary[Registration]
    ) { (registration: Registration) =>
      val updatedRegistration: Registration =
        registration.copy(
          entityType = None
        )

      pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
        routes.NotableErrorController.answersAreInvalid()
    }

    "(Check mode) return a call to the CheckYourAnswers page if entity type is Charity and charity registration number is present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength),
      stringsWithMaxLength(charityRegistrationNumberMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String,
        charityRegNo: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            charityRegistrationNumber = Some(charityRegNo)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad(updatedRegistration.registrationType.getOrElse(Initial))
    }

    "(Check Mode) return a call to the charity registration number page if entity type is Charity and charity registration number is not present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            charityRegistrationNumber = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Charity),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CharityRegistrationNumberController.onPageLoad(CheckMode)
    }

    "(Check Mode) return a call to the CheckYourAnswers page if entity type is UnincorporatedAssociation and company registration number is present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength),
      stringsWithMaxLength(companyRegistrationNumberMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String,
        companyRegNo: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            companyRegistrationNumber = Some(companyRegNo)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(UnincorporatedAssociation),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad(updatedRegistration.registrationType.getOrElse(Initial))
    }

    "(Check mode)return a call to the do you have crn page if entity type is UnincorporatedAssociation and company registration number is not present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            companyRegistrationNumber = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(UnincorporatedAssociation),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.DoYouHaveCrnController.onPageLoad(CheckMode)
    }

    "(Check Mode) return a call to the CheckYourAnswers page if entity type is NonUkEstablishment and company registration number is present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength),
      stringsWithMaxLength(companyRegistrationNumberMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String,
        companyRegNo: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            companyRegistrationNumber = Some(companyRegNo)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(NonUKEstablishment),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad(updatedRegistration.registrationType.getOrElse(Initial))
    }

    "(Check mode) return a call to the do you have crn page if entity type is NonUkEstablishment and company registration number is not present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            companyRegistrationNumber = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(NonUKEstablishment),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.DoYouHaveCrnController.onPageLoad(CheckMode)
    }

    "(Check Mode) return a call to the CheckYourAnswers page if entity type is Trust is selected and ctUtr is present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength),
      stringsWithMaxLength(utrLength)
    ) {
      (
        registration: Registration,
        businessName: String,
        ctUtr: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            ctUtr = Some(ctUtr)
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Trust),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad(updatedRegistration.registrationType.getOrElse(Initial))
    }

    "(Check mode) return a call to the charity registration number page if entity type is Trust and company registration number is not present" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(organisationNameMaxLength)
    ) {
      (
        registration: Registration,
        businessName: String
      ) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            businessName = Some(businessName),
            ctUtr = None
          )

        val updatedRegistration: Registration =
          registration.copy(
            entityType = Some(Trust),
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CtUtrController.onPageLoad(CheckMode)
    }

  }

}

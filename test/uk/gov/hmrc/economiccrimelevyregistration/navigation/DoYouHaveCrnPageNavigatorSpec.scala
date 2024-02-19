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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models._

class DoYouHaveCrnPageNavigatorSpec extends SpecBase {

  val pageNavigator = new DoYouHaveCrnPageNavigator()

  def nextPage(value: Boolean, registration: Registration): Call = value match {
    case true                                                                 => routes.NonUkCrnController.onPageLoad(NormalMode)
    case false if registration.entityType.contains(UnincorporatedAssociation) =>
      routes.DoYouHaveUtrController.onPageLoad(NormalMode)
    case false                                                                => routes.UtrTypeController.onPageLoad(NormalMode)
  }

  "nextPage" should {
    "return a Call to the next page in NormalMode" in forAll { (registration: Registration, hasUkCrn: Boolean) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isUkCrnPresent = Some(hasUkCrn)
        )

      val updatedRegistration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe
        nextPage(hasUkCrn, updatedRegistration)
    }

    "return a Call to the check your answers page in CheckMode" in forAll {
      (registration: Registration, hasUkCrn: Boolean) =>
        val otherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isUkCrnPresent = Some(hasUkCrn),
            companyRegistrationNumber = Some("")
          )

        val updatedRegistration =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    "return a Call to the Non UK CRN page in CheckMode" in forAll { registration: Registration =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          isUkCrnPresent = Some(true)
        )

      val updatedRegistration =
        registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

      pageNavigator.nextPage(CheckMode, updatedRegistration) shouldBe
        routes.NonUkCrnController.onPageLoad(CheckMode)
    }
  }

  "return a Call to the error page if no data present" in forAll { (registration: Registration, mode: Mode) =>
    val otherEntityJourneyData = OtherEntityJourneyData
      .empty()

    val updatedRegistration =
      registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

    pageNavigator.nextPage(mode, updatedRegistration) shouldBe
      routes.NotableErrorController.answersAreInvalid()
  }
}

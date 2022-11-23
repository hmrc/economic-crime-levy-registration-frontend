/*
 * Copyright 2022 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Gen
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, CheckMode, FinancialConductAuthority, GamblingCommission, Hmrc, NormalMode, Other, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.pages.{AmlSupervisorPage, Page, UkRevenuePage}

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "nextPage" should {
    "go to the AML Supervisor page from the UK revenue page in NormalMode when the entity meets the revenue threshold" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(true))

        navigator.nextPage(UkRevenuePage, NormalMode, updatedRegistration) shouldBe routes.AmlSupervisorController
          .onPageLoad()
    }

    "go to the not liable page from the UK revenue page in NormalMode when the entity does not meet the revenue threshold" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(false))

        navigator.nextPage(UkRevenuePage, NormalMode, updatedRegistration) shouldBe routes.NotLiableController
          .onPageLoad()
    }

    "go to the register with your AML Supervisor page in NormalMode when either the Gambling Commission or Financial Conduct Authority AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType      = Gen.oneOf[AmlSupervisorType](Seq(GamblingCommission, FinancialConductAuthority)).sample.get
        val amlSupervisor       = AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = None)
        val updatedRegistration = registration.copy(amlSupervisor = Some(amlSupervisor))

        navigator.nextPage(
          AmlSupervisorPage,
          NormalMode,
          updatedRegistration
        ) shouldBe routes.RegisterWithOtherAmlSupervisorController.onPageLoad()
    }

    "go to the select entity type page in NormalMode when either the HMRC or Other AML Supervisor option is selected" in forAll {
      registration: Registration =>
        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample
        val amlSupervisor         =
          AmlSupervisor(supervisorType = supervisorType, otherProfessionalBody = otherProfessionalBody)
        val updatedRegistration   = registration.copy(amlSupervisor = Some(amlSupervisor))

        navigator.nextPage(
          AmlSupervisorPage,
          NormalMode,
          updatedRegistration
        ) shouldBe routes.EntityTypeController.onPageLoad()
    }

    "go to the start page when the registration data does not contain the data required to trigger the correct routing" in forAll {
      (page: Page, internalId: String) =>
        val registration = Registration.empty(internalId)

        navigator.nextPage(page, NormalMode, registration) shouldBe routes.StartController.onPageLoad()
    }

    "go from a page that doesn't exist in the route map to Index in NormalMode" in forAll {
      registration: Registration =>
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, registration) shouldBe routes.StartController.onPageLoad()
    }

    "go from a page that doesn't exist in the edit route map to CheckYourAnswers in CheckMode" in forAll {
      registration: Registration =>
        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          registration
        ) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }
  }
}

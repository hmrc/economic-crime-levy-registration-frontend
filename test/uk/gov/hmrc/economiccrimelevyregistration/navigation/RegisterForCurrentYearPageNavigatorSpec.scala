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
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

import java.time.LocalDate

class RegisterForCurrentYearPageNavigatorSpec extends SpecBase {

  val pageNavigator = new RegisterForCurrentYearPageNavigator()

  "navigateInNormalMode" should {
    "return a call to the Aml Regulated Activity page when user is registering for current year" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo) =>
        val updatedAdditionalInfo    = additionalInfo.copy(registeringForCurrentYear = Some(true))
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(NormalMode, updatedRegistrationModel) shouldBe
          routes.AmlRegulatedActivityController.onPageLoad(NormalMode)
    }

    "return a call to the liability date page if user is not registering for current year" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo) =>
        val updatedAdditionalInfo    = additionalInfo.copy(registeringForCurrentYear = Some(false))
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(NormalMode, updatedRegistrationModel) shouldBe
          routes.LiabilityDateController.onPageLoad(NormalMode)
    }

    "return a call to the answers are invalid page if user has not provided an answer" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo) =>
        val updatedAdditionalInfo    = additionalInfo.copy(registeringForCurrentYear = None)
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(NormalMode, updatedRegistrationModel) shouldBe
          routes.NotableErrorController.answersAreInvalid()
    }
  }
  "navigateInCheckMode"  should {
    "return a call to the Aml Regulated Activity page in Normal Mode when user is registering for current year" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo) =>
        val updatedAdditionalInfo    = additionalInfo.copy(registeringForCurrentYear = Some(true))
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(NormalMode, updatedRegistrationModel) shouldBe
          routes.AmlRegulatedActivityController.onPageLoad(NormalMode)
    }

    "return a call to the check your answers page when user is not registering for current year and their liability start date is defined" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo, date: LocalDate) =>
        val updatedAdditionalInfo    =
          additionalInfo.copy(liabilityStartDate = Some(date), registeringForCurrentYear = Some(false))
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(CheckMode, updatedRegistrationModel) shouldBe
          routes.CheckYourAnswersController.onPageLoad(
            updatedRegistrationModel.registration.registrationType.getOrElse(Initial)
          )
    }

    "return a call to the liability date page if user is not registering for current year and liability start date is not defined" in forAll {
      (eclRegistrationModel: EclRegistrationModel, additionalInfo: RegistrationAdditionalInfo) =>
        val updatedAdditionalInfo    =
          additionalInfo.copy(liabilityStartDate = None, registeringForCurrentYear = Some(false))
        val updatedRegistrationModel =
          eclRegistrationModel.copy(registrationAdditionalInfo = Some(updatedAdditionalInfo))

        pageNavigator.nextPage(CheckMode, updatedRegistrationModel) shouldBe
          routes.LiabilityDateController.onPageLoad(CheckMode)
    }

  }

}

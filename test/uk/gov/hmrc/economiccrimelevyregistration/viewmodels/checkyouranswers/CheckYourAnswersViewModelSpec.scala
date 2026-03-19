/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkyouranswers

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers.CheckYourAnswersViewModel
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import java.time.LocalDate

class CheckYourAnswersViewModelSpec extends SpecBase {
  "organisationDetails" should {
    "contains liability start date when it is present in the subscription response" in forAll {
      (
        registration: Registration,
        getSubscriptionResponse: GetSubscriptionResponse,
        additionalInfo: RegistrationAdditionalInfo
      ) =>
        val updatedSubcriptionResponse = getSubscriptionResponse.copy(additionalDetails =
          getSubscriptionResponse.additionalDetails.copy(liabilityStartDate = "2018-12-27")
        )
        val updatedAdditionalInfo      = additionalInfo.copy(liabilityStartDate = None)

        val viewModel = CheckYourAnswersViewModel(
          registration.copy(registrationType = Some(Amendment)),
          Some(updatedSubcriptionResponse),
          Some(testEclRegistrationReference),
          Some(updatedAdditionalInfo)
        )

        val liabilityRow = viewModel.getLiabilityRow(messages)

        viewModel
          .organisationDetails()(messages)
          .rows
          .contains(liabilityRow.get) shouldBe true
    }

    "contains liability start date when it is present in the additional info" in forAll {
      (
        registration: Registration,
        additionalInfo: RegistrationAdditionalInfo,
        liabilityStartDate: LocalDate
      ) =>
        val additionalInfoWithLiabilityDate = additionalInfo.copy(liabilityStartDate = Some(liabilityStartDate))

        val viewModel = CheckYourAnswersViewModel(
          registration.copy(registrationType = Some(Amendment)),
          None,
          Some(testEclRegistrationReference),
          Some(additionalInfoWithLiabilityDate)
        )

        val liabilityRow = viewModel.getLiabilityRow(messages)

        viewModel
          .organisationDetails()(messages)
          .rows
          .contains(liabilityRow.get) shouldBe true
    }
  }

}

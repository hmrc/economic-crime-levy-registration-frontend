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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, Mode, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class RelevantAp12MonthsPageNavigatorSpec extends SpecBase {

  val pageNavigator = new RelevantAp12MonthsPageNavigator

  "nextPage" should {
    "return a Call to the UK revenue page from the relevant AP 12 months page in either mode when the 'Yes' option is selected" in forAll {
      (registration: Registration, mode: Mode, hasRegistrationChanged: Boolean) =>
        val updatedRegistration = registration.copy(relevantAp12Months = Some(true))

        val nextPage = mode match {
          case NormalMode => routes.UkRevenueController.onPageLoad(mode)
          case CheckMode  =>
            if (hasRegistrationChanged) {
              routes.UkRevenueController.onPageLoad(mode)
            } else {
              routes.CheckYourAnswersController.onPageLoad()
            }
        }

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(
            registration = updatedRegistration,
            hasRegistrationChanged = hasRegistrationChanged
          )
        ) shouldBe nextPage
    }

    "return a Call to the relevant AP length page from the relevant AP 12 months page in either mode when the 'No' option is selected" in forAll {
      (registration: Registration, mode: Mode, hasRegistrationChanged: Boolean) =>
        val updatedRegistration = registration.copy(relevantAp12Months = Some(false))

        val nextPage = mode match {
          case NormalMode => routes.RelevantApLengthController.onPageLoad(mode)
          case CheckMode  =>
            if (hasRegistrationChanged) {
              if (updatedRegistration.relevantApLength.isEmpty) {
                routes.RelevantApLengthController.onPageLoad(mode)
              } else {
                routes.UkRevenueController.onPageLoad(mode)
              }
            } else {
              routes.CheckYourAnswersController.onPageLoad()
            }
        }

        pageNavigator.nextPage(
          mode,
          EclRegistrationModel(
            registration = updatedRegistration,
            hasRegistrationChanged = hasRegistrationChanged
          )
        ) shouldBe nextPage
    }
  }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, Mode, NormalMode, Registration}

class RelevantAp12MonthsPageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigate(eclRegistrationModel.registration, NormalMode)

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigate(eclRegistrationModel.registration, CheckMode, eclRegistrationModel.hasRegistrationChanged)

  private def navigate(registration: Registration, mode: Mode, hasRegistrationChanged: Boolean = true): Call =
    if (hasRegistrationChanged) {
      (mode, registration.relevantAp12Months) match {
        case (NormalMode, Some(true))  =>
          routes.UkRevenueController.onPageLoad(mode)
        case (NormalMode, Some(false)) =>
          routes.RelevantApLengthController.onPageLoad(mode)
        case (CheckMode, Some(true))   =>
          routes.UkRevenueController.onPageLoad(mode)
        case (CheckMode, Some(false))  =>
          if (registration.relevantApLength.isEmpty) {
            routes.RelevantApLengthController.onPageLoad(mode)
          } else {
            routes.UkRevenueController.onPageLoad(mode)
          }
        case _                         => routes.NotableErrorController.answersAreInvalid()
      }
    } else {
      routes.CheckYourAnswersController.onPageLoad()
    }

}

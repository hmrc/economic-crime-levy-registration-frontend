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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

import javax.inject.Inject

class UkRevenuePageNavigator @Inject() () extends PageNavigator {
  override protected def navigateInNormalMode(registration: Registration): Call = navigate(NormalMode, registration)

  override protected def navigateInCheckMode(registration: Registration): Call = navigate(CheckMode, registration)

  private def navigate(mode: Mode, registration: Registration): Call =
    registration.relevantApRevenue match {
      case Some(_) =>
        registration.revenueMeetsThreshold match {
          case Some(true)  =>
            mode match {
              case NormalMode => routes.EntityTypeController.onPageLoad(NormalMode)
              case CheckMode  => routes.CheckYourAnswersController.onPageLoad()
            }
          case Some(false) => routes.NotLiableController.onPageLoad()
          case _           => routes.JourneyRecoveryController.onPageLoad()
        }
      case _       => routes.JourneyRecoveryController.onPageLoad()
    }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode}

import javax.inject.Inject

class DoYouHaveUtrPageNavigator @Inject() extends PageNavigator {
  override protected def navigateInNormalMode(navigationData: NavigationData): Call =
    navigationData.registration.otherEntityJourneyData.isCtUtrPresent match {
      case Some(isCtUtrPresent) =>
        navigateInEitherMode(isCtUtrPresent, navigationData.registration.otherEntityJourneyData.ctUtr, NormalMode)
      case None                 => routes.NotableErrorController.answersAreInvalid()
    }

  override protected def navigateInCheckMode(navigationData: NavigationData): Call =
    navigationData.registration.otherEntityJourneyData.isCtUtrPresent match {
      case Some(isCtUtrPresent) =>
        navigateInEitherMode(isCtUtrPresent, navigationData.registration.otherEntityJourneyData.ctUtr, CheckMode)
      case None                 => routes.NotableErrorController.answersAreInvalid()
    }

  private def navigateInEitherMode(isCtUtrPresent: Boolean, utr: Option[String], mode: Mode): Call =
    if (isCtUtrPresent) {
      mode match {
        case CheckMode  =>
          utr match {
            case Some(_) => routes.CheckYourAnswersController.onPageLoad()
            case None    => routes.UtrController.onPageLoad(mode)
          }
        case NormalMode => routes.UtrController.onPageLoad(mode)
      }
    } else {
      mode match {
        case NormalMode => routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)
        case CheckMode  => routes.CheckYourAnswersController.onPageLoad()
      }
    }
}

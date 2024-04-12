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
import uk.gov.hmrc.economiccrimelevyregistration.models._

import javax.inject.Inject

class DoYouHaveUtrPageNavigator @Inject() extends PageNavigator {
  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigateInEitherMode(eclRegistrationModel.registration, NormalMode)

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigateInEitherMode(eclRegistrationModel.registration, CheckMode)

  private def navigateInEitherMode(registration: Registration, mode: Mode): Call =
    if (registration.isUnincorporatedAssociation) {
      routeUnincorporatedAssociation(registration, mode)
    } else {
      routeOtherEntityTypes(registration, mode)
    }

  private def routeUnincorporatedAssociation(registration: Registration, mode: Mode) =
    if (registration.otherEntityJourneyData.isCtUtrPresent.contains(true)) {
      mode match {
        case CheckMode  =>
          registration.otherEntityJourneyData.ctUtr match {
            case Some(_) =>
              routes.CheckYourAnswersController.onPageLoad()
            case None    =>
              routes.UtrTypeController.onPageLoad(mode)
          }
        case NormalMode =>
          routes.UtrTypeController.onPageLoad(mode)
      }
    } else {
      mode match {
        case NormalMode =>
          routes.BusinessSectorController.onPageLoad(mode)
        case CheckMode  =>
          routes.CheckYourAnswersController.onPageLoad()
      }
    }

  private def routeOtherEntityTypes(registration: Registration, mode: Mode) =
    if (registration.otherEntityJourneyData.isCtUtrPresent.contains(true)) {
      mode match {
        case NormalMode =>
          routes.UtrController.onPageLoad(mode)
        case CheckMode  =>
          registration.otherEntityJourneyData.ctUtr match {
            case Some(_) =>
              routes.CheckYourAnswersController.onPageLoad()
            case None    =>
              routes.UtrController.onPageLoad(mode)
          }
      }
    } else {
      mode match {
        case NormalMode =>
          routes.CompanyRegistrationNumberController.onPageLoad(mode)
        case CheckMode  =>
          if (registration.otherEntityJourneyData.companyRegistrationNumber.isEmpty) {
            routes.CompanyRegistrationNumberController.onPageLoad(mode)
          } else {
            routes.CheckYourAnswersController.onPageLoad()
          }
      }
    }
}

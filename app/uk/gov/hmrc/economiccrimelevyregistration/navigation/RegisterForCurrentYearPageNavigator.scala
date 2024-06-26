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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode}

class RegisterForCurrentYearPageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    eclRegistrationModel.registrationAdditionalInfo match {
      case Some(additionalInfo) =>
        additionalInfo.registeringForCurrentYear match {
          case Some(value) =>
            if (value) {
              routes.AmlRegulatedActivityController.onPageLoad(NormalMode)
            } else {
              routes.LiabilityDateController.onPageLoad(NormalMode)
            }
          case None        => routes.NotableErrorController.answersAreInvalid()
        }
      case None                 => routes.NotableErrorController.answersAreInvalid()
    }

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
    if (eclRegistrationModel.hasAdditionalInfoChanged) {
      eclRegistrationModel.registrationAdditionalInfo match {
        case Some(additionalInfo) =>
          additionalInfo.registeringForCurrentYear match {
            case Some(value) =>
              if (value) {
                routes.AmlRegulatedActivityController.onPageLoad(NormalMode)
              } else {
                if (additionalInfo.liabilityStartDate.isDefined) {
                  routes.CheckYourAnswersController.onPageLoad()
                } else {
                  routes.LiabilityDateController.onPageLoad(CheckMode)
                }
              }
            case None        => routes.NotableErrorController.answersAreInvalid()
          }
        case None                 => routes.NotableErrorController.answersAreInvalid()
      }
    } else {
      routes.CheckYourAnswersController.onPageLoad()
    }
}

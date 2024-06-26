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

import javax.inject.Inject

class AmlRegulatedActivityPageNavigator @Inject() extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigateInBothModes(eclRegistrationModel.registration, NormalMode)

  override protected def navigateInCheckMode(
    eclRegistrationModel: EclRegistrationModel
  ): Call = if (eclRegistrationModel.hasRegistrationChanged) {
    navigateInBothModes(eclRegistrationModel.registration, CheckMode)
  } else {
    routes.CheckYourAnswersController.onPageLoad()
  }

  private def navigateInBothModes(registration: Registration, mode: Mode): Call =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(true)  =>
        routes.AmlSupervisorController.onPageLoad(mode, registration.registrationType.get)
      case Some(false) =>
        routes.LiabilityBeforeCurrentYearController.onPageLoad(mode)
      case _           =>
        routes.NotableErrorController.answersAreInvalid()
    }
}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, Mode, NormalMode}

import javax.inject.Inject

class ConfirmContactAddressPageNavigator @Inject() () extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigate(eclRegistrationModel, NormalMode)

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigate(eclRegistrationModel, CheckMode)

  private def navigate(
    eclRegistrationModel: EclRegistrationModel,
    mode: Mode
  ): Call =
    eclRegistrationModel.registration.useRegisteredOfficeAddressAsContactAddress match {
      case Some(true)  =>
        routes.CheckYourAnswersController.onPageLoad()
      case Some(false) =>
        routes.IsUkAddressController.onPageLoad(mode)
      case _           =>
        routes.NotableErrorController.answersAreInvalid()
    }

}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial

class LiabilityDatePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    eclRegistrationModel.registration.amlSupervisor match {
      case None =>
        routes.AmlSupervisorController
          .onPageLoad(NormalMode, eclRegistrationModel.registration.registrationType.getOrElse(Initial))
      case _    => routes.EntityTypeController.onPageLoad(NormalMode)
    }

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
    routes.CheckYourAnswersController.onPageLoad()
}

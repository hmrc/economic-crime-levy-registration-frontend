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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, Mode, NormalMode, Registration}

class NonUkCrnPageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
    navigateInMode(eclRegistrationModel.registration, NormalMode)

  private def navigateInMode(registration: Registration, mode: Mode) =
    if (registration.entityType.contains(UnincorporatedAssociation)) {
      routes.DoYouHaveUtrController.onPageLoad(mode)
    } else {
      routes.UtrTypeController.onPageLoad(mode)
    }

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call = {
    val registration = eclRegistrationModel.registration
    if (
      (registration.isUnincorporatedAssociation && registration.otherEntityJourneyData.isCtUtrPresent.isEmpty)
      || (!registration.isUnincorporatedAssociation && registration.otherEntityJourneyData.utrType.isEmpty)
    ) {
      navigateInMode(registration, CheckMode)
    } else {
      routes.CheckYourAnswersController.onPageLoad()
    }
  }
}

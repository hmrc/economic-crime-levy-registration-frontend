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
import uk.gov.hmrc.economiccrimelevyregistration.models.UtrType.{CtUtr, SaUtr}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode}

class UtrTypePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = {
    val otherEntityJourneyData = eclRegistrationModel.registration.otherEntityJourneyData
    otherEntityJourneyData.utrType match {
      case Some(CtUtr) => routes.CtUtrController.onPageLoad(NormalMode)
      case Some(SaUtr) => routes.SaUtrController.onPageLoad(NormalMode)
      case _           => routes.NotableErrorController.answersAreInvalid()
    }
  }

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call = {
    val otherEntityJourneyData = eclRegistrationModel.registration.otherEntityJourneyData
    otherEntityJourneyData.utrType match {
      case Some(CtUtr) if otherEntityJourneyData.ctUtr.isEmpty => routes.CtUtrController.onPageLoad(CheckMode)
      case Some(SaUtr) if otherEntityJourneyData.saUtr.isEmpty => routes.SaUtrController.onPageLoad(CheckMode)
      case _                                                   =>
        routes.CheckYourAnswersController.onPageLoad()
    }
  }
}

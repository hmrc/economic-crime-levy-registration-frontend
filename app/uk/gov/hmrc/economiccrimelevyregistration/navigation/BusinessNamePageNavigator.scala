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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

class BusinessNamePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(registration: Registration): Call =
    navigateInMode(registration, NormalMode)

  private def navigateInMode(registration: Registration, mode: Mode) =
    registration.entityType match {
      case Some(value) =>
        value match {
          case Charity                   => routes.CharityRegistrationNumberController.onPageLoad(mode)
          case UnincorporatedAssociation => routes.DoYouHaveCtUtrController.onPageLoad(mode)
          case Trust                     => routes.CtUtrController.onPageLoad(mode)
          case NonUKEstablishment        => routes.DoYouHaveCrnController.onPageLoad(mode)
          case _                         => error()
        }
      case _           => error()
    }

  override protected def navigateInCheckMode(registration: Registration): Call = {
    val otherEntityJourneyData = registration.otherEntityJourneyData
    val isNextFieldEmpty       = registration.entityType match {
      case Some(value) =>
        value match {
          case Charity                   => otherEntityJourneyData.charityRegistrationNumber.isEmpty
          case UnincorporatedAssociation => otherEntityJourneyData.isCtUtrPresent.isEmpty
          case Trust                     => otherEntityJourneyData.ctUtr.isEmpty
          case NonUKEstablishment        => otherEntityJourneyData.companyRegistrationNumber.isEmpty
          case _                         => false
        }
      case _           => false
    }
    if (isNextFieldEmpty) {
      navigateInMode(registration, CheckMode)
    } else {
      routes.CheckYourAnswersController.onPageLoad()
    }
  }

  private def error() =
    routes.NotableErrorController.answersAreInvalid()
}

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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{NonUKEstablishment, Trust}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CtUtrPageNavigator extends PageNavigator {
  override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = {
    val registration = eclRegistrationModel.registration

    registration.otherEntityJourneyData.ctUtr match {
      case None    => routes.NotableErrorController.answersAreInvalid()
      case Some(_) =>
        registration.entityType match {
          case None                        => routes.NotableErrorController.answersAreInvalid()
          case Some(_ @Trust)              => routes.BusinessSectorController.onPageLoad(NormalMode)
          case Some(_ @NonUKEstablishment) => routes.BusinessSectorController.onPageLoad(NormalMode)
          case Some(_)                     => routes.CtUtrPostcodeController.onPageLoad(NormalMode)
        }
    }
  }

  override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call = {
    val registration = eclRegistrationModel.registration

    registration.entityType match {
      case None                        => routes.NotableErrorController.answersAreInvalid()
      case Some(_ @Trust)              =>
        routes.CheckYourAnswersController.onPageLoad()
      case Some(_ @NonUKEstablishment) =>
        routes.CheckYourAnswersController.onPageLoad()
      case Some(_)                     =>
        registration.otherEntityJourneyData.isCtUtrPresent match {
          case None           => routes.NotableErrorController.answersAreInvalid()
          case Some(_ @ true) =>
            registration.otherEntityJourneyData.postcode match {
              case None    => routes.CtUtrPostcodeController.onPageLoad(CheckMode)
              case Some(_) =>
                routes.CheckYourAnswersController.onPageLoad()
            }
          case Some(_)        =>
            routes.CheckYourAnswersController.onPageLoad()
        }
    }
  }

}

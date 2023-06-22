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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class DoYouHaveCtUtrPageNavigator @Inject() (implicit
  ex: ExecutionContext
) extends PageNavigator {
  override protected def navigateInNormalMode(registration: Registration): Call =
    registration.otherEntityJourneyData.isCtUtrPresent match {
      case Some(isCtUtrPresent) =>
        navigateInEitherMode(registration.otherEntityJourneyData.postcode, isCtUtrPresent, NormalMode)
      case None                 => routes.NotableErrorController.answersAreInvalid()
    }

  override protected def navigateInCheckMode(registration: Registration): Call =
    registration.otherEntityJourneyData.isCtUtrPresent match {
      case Some(isCtUtrPresent) =>
        navigateInEitherMode(registration.otherEntityJourneyData.postcode, isCtUtrPresent, CheckMode)
      case None                 => routes.NotableErrorController.answersAreInvalid()
    }

  private def navigateInEitherMode(postcode: Option[String], isCtUtrPresent: Boolean, mode: Mode): Call =
    if (isCtUtrPresent) {
      (mode, postcode) match {
        case (CheckMode, Some(_)) => routes.OtherEntityCheckYourAnswersController.onPageLoad()
        case (CheckMode, None)    => routes.CtUtrController.onPageLoad(mode)
        case (NormalMode, _)      => routes.CtUtrController.onPageLoad(mode)
      }
    } else {
      routes.OtherEntityCheckYourAnswersController.onPageLoad()
    }
}

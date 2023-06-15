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
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class DoYouHaveCTUTRPageNavigator @Inject() (implicit
  ex: ExecutionContext
) extends PageNavigator {
  override protected def navigateInNormalMode(registration: Registration): Call = navigateInEitherMode(registration)

  override protected def navigateInCheckMode(registration: Registration): Call = navigateInEitherMode(registration)

  private def navigateInEitherMode(registration: Registration): Call =
    if (registration.otherEntityJourneyData.ctUtr.get.toBoolean) {
      routes.AddCTUTRController.onPageLoad()
    } else {
      routes.CheckYourAnswersController.onPageLoad()
    }
}

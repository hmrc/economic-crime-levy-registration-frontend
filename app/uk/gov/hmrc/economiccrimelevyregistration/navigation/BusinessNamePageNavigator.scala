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

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.Future

class BusinessNamePageNavigator extends AsyncPageNavigator with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] = Future.successful(registration.otherEntityJourneyData.entityType match {
    case Some(value) =>
      value match {
        case Charity => routes.CharityRegistrationNumberController.onPageLoad(NormalMode)
        case _       => ???
      }
  })

  override protected def navigateInCheckMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] = Future.successful(routes.CheckYourAnswersController.onPageLoad())
}

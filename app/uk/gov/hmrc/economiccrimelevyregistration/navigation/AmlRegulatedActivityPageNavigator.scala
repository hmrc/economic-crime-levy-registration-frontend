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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmlRegulatedActivityPageNavigator @Inject() (eclRegistrationConnector: EclRegistrationConnector)(implicit
  ec: ExecutionContext
) extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(
    registration: Registration
  )(implicit request: RequestHeader): Future[Call] =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(true)  => Future.successful(routes.AmlSupervisorController.onPageLoad(NormalMode))
      case Some(false) => notLiable(registration.internalId)
      case _           => Future.successful(routes.JourneyRecoveryController.onPageLoad())
    }

  override protected def navigateInCheckMode(
    registration: Registration
  )(implicit request: RequestHeader): Future[Call] =
    registration.carriedOutAmlRegulatedActivityInCurrentFy match {
      case Some(true)  => Future.successful(routes.CheckYourAnswersController.onPageLoad())
      case Some(false) => notLiable(registration.internalId)
      case _           => Future.successful(routes.JourneyRecoveryController.onPageLoad())
    }

  private def notLiable(internalId: String)(implicit request: RequestHeader): Future[Call] =
    eclRegistrationConnector.deleteRegistration(internalId).map(_ => routes.NotLiableController.onPageLoad())

}

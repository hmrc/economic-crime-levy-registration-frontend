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
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkRevenuePageNavigator @Inject() (auditConnector: AuditConnector)(implicit ec: ExecutionContext)
    extends AsyncPageNavigator {
  override protected def navigateInNormalMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] = navigate(NormalMode, registration)

  override protected def navigateInCheckMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] = navigate(CheckMode, registration)

  private def navigate(mode: Mode, registration: Registration): Future[Call] =
    registration.relevantApRevenue match {
      case Some(_) =>
        registration.revenueMeetsThreshold match {
          case Some(true)  =>
            mode match {
              case NormalMode => Future.successful(routes.EntityTypeController.onPageLoad(NormalMode))
              case CheckMode  => Future.successful(routes.CheckYourAnswersController.onPageLoad())
            }
          case Some(false) =>
            auditConnector
              .sendExtendedEvent(
                RegistrationNotLiableAuditEvent(
                  registration.internalId,
                  NotLiableReason.RevenueDoesNotMeetThreshold.toString
                ).extendedDataEvent
              )

            Future.successful(routes.NotLiableController.onPageLoad())
          case _           => Future.successful(routes.NotableErrorController.answersAreInvalid())
        }
      case _       => Future.successful(routes.NotableErrorController.answersAreInvalid())
    }

}

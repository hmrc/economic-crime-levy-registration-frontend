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
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LiabilityBeforeCurrentYearPageNavigator @Inject() (auditConnector: AuditConnector)(implicit
  ex: ExecutionContext
) extends PageNavigator {
  def nextPage(liableBeforeCurrentYear: Boolean, registration: Registration): Call =
    if (liableBeforeCurrentYear) {
      routes.EntityTypeController.onPageLoad(NormalMode)
    } else {
      registration.relevantApRevenue match {
        case Some(_) =>
          registration.revenueMeetsThreshold match {
            case Some(true) => routes.EntityTypeController.onPageLoad(NormalMode)
            case _          => sendNotLiableAuditEvent(registration.internalId)
          }
        case None    => sendNotLiableAuditEvent(registration.internalId)
      }
    }

  private def sendNotLiableAuditEvent(internalId: String): Call = {
    auditConnector
      .sendExtendedEvent(
        RegistrationNotLiableAuditEvent(
          internalId,
          NotLiableReason.DidNotCarryOutAmlRegulatedActivity
        ).extendedDataEvent
      )

    routes.NotLiableController.youDoNotNeedToRegister()
  }

  override protected def navigateInNormalMode(registration: Registration): Call =
    throw new Exception("Invalid call!")

  override protected def navigateInCheckMode(registration: Registration): Call =
    throw new Exception("Invalid call!")

}

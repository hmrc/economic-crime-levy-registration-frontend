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
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class LiabilityBeforeCurrentYearPageNavigator @Inject() (auditConnector: AuditConnector)(implicit
  ex: ExecutionContext
) extends PageNavigator {
  def nextPage(
    liableBeforeCurrentYear: Boolean,
    registration: Registration,
    mode: Mode,
    fromRevenuePage: Boolean
  ): Call =
    mode match {
      case NormalMode =>
        if (liableBeforeCurrentYear) {
          fromRevenuePage match {
            case true => routes.EntityTypeController.onPageLoad(NormalMode)
            case false => routes.AmlSupervisorController.onPageLoad(NormalMode, registration.registrationType.get, true)
          }
        } else if (fromRevenuePage) {
          registration.revenueMeetsThreshold match {
            case Some(true) => routes.EntityTypeController.onPageLoad(NormalMode)
            case _          => sendNotLiableAuditEvent(registration.internalId)
          }
        } else {
          sendNotLiableAuditEvent(registration.internalId)
        }
      case CheckMode  => routes.CheckYourAnswersController.onPageLoad()
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

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

package uk.gov.hmrc.economiccrimelevyregistration.services

import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationStartedEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector,
  auditConnector: AuditConnector
)(implicit
  ec: ExecutionContext
) {
  def getOrCreateRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Registration] =
    eclRegistrationConnector.getRegistration(internalId).flatMap {
      case Some(registration) => Future.successful(registration)
      case None               =>
        auditConnector
          .sendExtendedEvent(
            RegistrationStartedEvent(
              internalId
            ).extendedDataEvent
          )

        eclRegistrationConnector.upsertRegistration(Registration.empty(internalId))
    }
}

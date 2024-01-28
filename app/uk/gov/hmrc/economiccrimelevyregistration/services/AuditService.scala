/*
 * Copyright 2024 HM Revenue & Customs
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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent, RegistrationStartedEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.AuditError
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class AuditService @Inject() (auditConnector: AuditConnector)(implicit
  ec: ExecutionContext
) {
  def sendEvent(extendedDataEvent: ExtendedDataEvent)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    EitherT {
      auditConnector
        .sendExtendedEvent(extendedDataEvent)
        .map(_ => Right(()))
        .recover { case e =>
          Left(AuditError.InternalUnexpectedError(e.getMessage, Some(e)))
        }
    }

  def sendNotLiableAuditEvent(internalId: String, notLiableReason: NotLiableReason)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    EitherT {
      auditConnector
        .sendExtendedEvent(RegistrationNotLiableAuditEvent(internalId, notLiableReason).extendedDataEvent)
        .map(_ => Right(()))
        .recover { case e =>
          Left(AuditError.InternalUnexpectedError(e.getMessage, Some(e)))
        }
    }

  def sendRegistrationStartedEvent(internalId: String): EitherT[Future, AuditError, Unit] =
    EitherT {
      auditConnector
        .sendExtendedEvent(RegistrationStartedEvent(internalId).extendedDataEvent)
        .map(_ => Right(()))
        .recover { case e =>
          Left(AuditError.InternalUnexpectedError(e.getMessage, Some(e)))
        }
    }
}

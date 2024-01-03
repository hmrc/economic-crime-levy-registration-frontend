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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationStartedEvent
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.RegistrationError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector,
  auditConnector: AuditConnector
)(implicit
  ec: ExecutionContext,
  hc: HeaderCarrier
) {
  //TODO: Change V1 and remove V2
  def getOrCreateRegistrationV2(internalId: String): EitherT[Future, RegistrationError, Registration] =
    EitherT {
      eclRegistrationConnector
        .getRegistration(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined =>
            Left(RegistrationError.BadGateway(message, code))
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            auditConnector.sendExtendedEvent(RegistrationStartedEvent(internalId).extendedDataEvent)
            Right(eclRegistrationConnector.upsertRegistration(Registration.empty(internalId)))
          case NonFatal(thr) => Left(RegistrationError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def upsertRegistration(registration: Registration): EitherT[Future, RegistrationError, Registration] =
    EitherT {
      eclRegistrationConnector
        .upsertRegistration(registration)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(RegistrationError.BadGateway(message, code))
          case NonFatal(thr) => Left(RegistrationError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

}

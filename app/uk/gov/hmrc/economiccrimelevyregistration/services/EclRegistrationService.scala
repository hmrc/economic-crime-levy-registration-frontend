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
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.RegistrationStartedEvent
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, RegistrationError}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector,
  incorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector,
  soleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector,
  partnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector,
  auditConnector: AuditConnector,
  addressLookupFrontendConnector: AddressLookupFrontendConnector
)(implicit
  ec: ExecutionContext
) {
  def getOrCreateRegistration(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, RegistrationError, Registration] =
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
            val registration = Registration.empty(internalId)
            eclRegistrationConnector.upsertRegistration(registration)
            Right(registration)
          case NonFatal(thr) => Left(RegistrationError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def upsertRegistration(
    registration: Registration
  )(implicit hc: HeaderCarrier): EitherT[Future, RegistrationError, Registration] =
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

  def deleteRegistration(
    internalId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      eclRegistrationConnector
        .deleteRegistration(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def submitRegistration(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, CreateEclSubscriptionResponse] =
    EitherT {
      eclRegistrationConnector
        .submitRegistration(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def getRegistrationValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      eclRegistrationConnector
        .getRegistrationValidationErrors(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def registerEntityType(
    entityType: EntityType,
    mode: Mode
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, String] =
    entityType match {
      case UkLimitedCompany | UnlimitedCompany | RegisteredSociety =>
        EitherT {
          incorporatedEntityIdentificationFrontendConnector
            .createIncorporatedEntityJourney(entityType, mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }

      case SoleTrader =>
        EitherT {
          soleTraderIdentificationFrontendConnector
            .createSoleTraderJourney(mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }

      case GeneralPartnership | ScottishPartnership | LimitedPartnership | ScottishLimitedPartnership |
          LimitedLiabilityPartnership =>
        EitherT {
          partnershipIdentificationFrontendConnector
            .createPartnershipJourney(entityType, mode)
            .map(response => Right(response.journeyStartUrl))
            .recover {
              case error @ UpstreamErrorResponse(message, code, _, _)
                  if UpstreamErrorResponse.Upstream5xxResponse
                    .unapply(error)
                    .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
                Left(DataRetrievalError.BadGateway(message, code))
              case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
            }
        }
    }
}

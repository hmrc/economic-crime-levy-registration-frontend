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
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyregistration.connectors.RegistrationAdditionalInfoConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RegistrationAdditionalInfoService @Inject() (
  registrationAdditionalInfoConnector: RegistrationAdditionalInfoConnector
)(implicit ec: ExecutionContext) {

  def getOrCreate(
    internalId: String,
    eclReference: Option[String]
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo] =
    get(internalId).flatMap {
      case Some(additionalInfo) =>
        EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](Future.successful(Right(additionalInfo)))
      case None                 =>
        val newAdditionalInfo = RegistrationAdditionalInfo(internalId, None, None, None, eclReference)
        upsert(newAdditionalInfo).map(_ => newAdditionalInfo)
    }

  def get(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, Option[RegistrationAdditionalInfo]] =
    EitherT {
      registrationAdditionalInfoConnector
        .get(internalId)
        .map(additionalInfo => Right(Some(additionalInfo)))
        .recover {
          case err: NotFoundException                          => Right(None)
          case err @ UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
            Right(None)
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr)                                   => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def upsert(
    info: RegistrationAdditionalInfo
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      registrationAdditionalInfoConnector
        .upsert(info)
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

  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      registrationAdditionalInfoConnector
        .delete(internalId)
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
}

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

package uk.gov.hmrc.economiccrimelevyregistration.services.deregister

import cats.data.EitherT
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.economiccrimelevyregistration.connectors.deregister.DeregistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class DeregistrationService @Inject() (
  deregistrationConnector: DeregistrationConnector
)(implicit ec: ExecutionContext) {

  def getOrCreate(
    internalId: String
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DataRetrievalError, Deregistration] =
    get(internalId).flatMap {
      case Some(deregistration) =>
        EitherT[Future, DataRetrievalError, Deregistration](Future.successful(Right(deregistration)))
      case None                 =>
        val deregistration = Deregistration.empty(internalId)
        upsert(deregistration).map(_ => deregistration)
    }

  def get(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, Option[Deregistration]] =
    EitherT {
      deregistrationConnector
        .getDeregistration(internalId)
        .map(deregistration => Right(Some(deregistration)))
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
    deregistration: Deregistration
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      deregistrationConnector
        .upsertDeregistration(deregistration)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def delete(
    internalId: String
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      deregistrationConnector
        .deleteDeregistration(internalId)
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

  def submit(
    internalId: String
  )(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DataRetrievalError, Unit] =
    EitherT {
      deregistrationConnector
        .submitDeregistration(internalId)
        .map(_ => Right(()))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(DataRetrievalError.BadGateway(message, code))
          case NonFatal(thr) => Left(DataRetrievalError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

}

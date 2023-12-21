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
import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyregistration.connectors.SessionDataConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SessionError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class SessionService @Inject() (sessionRetrievalConnector: SessionDataConnector)(implicit ec: ExecutionContext) {

  def get(session: Session, internalId: String, key: String)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, String] =
    Try {
      session(key)
    } match {
      case Success(value) => EitherT.rightT(value)
      case Failure(_)     =>
        for {
          sessionData <- getSessionData(internalId)
          value       <- retrieveValueFromSessionData(sessionData, key)
        } yield value

    }

  def upsert(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Unit]      =
    for {
      oldSession    <- getSessionData(sessionData.internalId, sessionData)
      newSessionData = sessionData.copy(values = sessionData.values ++ oldSession.values)
      _             <- upsertSessions(newSessionData)
    } yield ()
  def delete(
    internalId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    sessionRetrievalConnector.delete(internalId).recover {
      case error @ UpstreamErrorResponse(message, code, _, _)
          if UpstreamErrorResponse.Upstream5xxResponse
            .unapply(error)
            .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
        Left(SessionError.BadGateway(message, code))
      case NonFatal(thr) =>
        Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
    }

  private def getSessionData(
    internalId: String,
    sessionData: SessionData
  )(implicit hc: HeaderCarrier): EitherT[Future, SessionError, SessionData] =
    EitherT {
      sessionRetrievalConnector
        .get(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            sessionRetrievalConnector.upsert(sessionData)
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr) =>
            sessionRetrievalConnector.upsert(sessionData)
            Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def getSessionData(
    internalId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, SessionError, SessionData] =
    EitherT {
      sessionRetrievalConnector
        .get(internalId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr) =>
            Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def retrieveValueFromSessionData(
    sessionData: SessionData,
    key: String
  ): EitherT[Future, SessionError, String] =
    EitherT {
      sessionData.values.get(key) match {
        case Some(value) => Future.successful(Right(value))
        case None        =>
          Future.successful(Left(SessionError.InternalUnexpectedError(s"Key not found in session: $key", None)))
      }
    }

  private def upsertSessions(sessionData: SessionData)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, SessionError, Unit] =
    EitherT {
      sessionRetrievalConnector
        .upsert(sessionData)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(SessionError.BadGateway(message, code))
          case NonFatal(thr) => Left(SessionError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}

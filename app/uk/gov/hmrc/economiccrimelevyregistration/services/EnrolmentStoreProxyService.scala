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
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, GroupEnrolmentsResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.EnrolmentStoreProxyError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {
  def getEclReferenceFromGroupEnrolment(
    groupId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, EnrolmentStoreProxyError, String] =
    for {
      response     <- getEnrolmentsForGroup(groupId)
      eclReference <- getEclReference(response)
    } yield eclReference

  private def getEnrolmentsForGroup(
    groupId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, EnrolmentStoreProxyError, GroupEnrolmentsResponse] =
    EitherT {
      enrolmentStoreProxyConnector
        .getEnrolmentsForGroup(groupId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(EnrolmentStoreProxyError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(EnrolmentStoreProxyError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  private def getEclReference(response: GroupEnrolmentsResponse): EitherT[Future, EnrolmentStoreProxyError, String] =
    EitherT {
      response.enrolments
        .find(_.service == EclEnrolment.serviceName)
        .flatMap(_.identifiers.find(_.key == EclEnrolment.identifierKey))
        .map(_.value) match {
        case Some(value) => Future.successful(Right(value))
        case None        =>
          Future.successful(Left(EnrolmentStoreProxyError.BadGateway("Unable to find an ecl reference", BAD_GATEWAY)))
      }
    }
}

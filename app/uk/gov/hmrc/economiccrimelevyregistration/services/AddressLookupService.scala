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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.AddressLookupContinueError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class AddressLookupService @Inject() (addressLookupFrontendConnector: AddressLookupFrontendConnector)(implicit
  ec: ExecutionContext
) {

  def initJourney(isUkAddress: Boolean, mode: Mode)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AddressLookupContinueError, String] =
    EitherT {
      addressLookupFrontendConnector
        .initJourney(isUkAddress, mode)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(AddressLookupContinueError.BadGateway(message, code))
          case NonFatal(thr) => Left(AddressLookupContinueError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

  def getAddress(
    addressId: String
  )(implicit hc: HeaderCarrier): EitherT[Future, AddressLookupContinueError, AlfAddressData] =
    EitherT {
      addressLookupFrontendConnector
        .getAddress(addressId)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse.unapply(error).isDefined ||
                UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(AddressLookupContinueError.BadGateway(message, code))
          case NonFatal(thr) => Left(AddressLookupContinueError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}

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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclCalculatorConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EclCalculatorService @Inject() (
  eclCalculatorConnector: EclCalculatorConnector
)(implicit ec: ExecutionContext) {
  def checkIfRevenueMeetsThreshold(
    registration: Registration
  )(implicit hc: HeaderCarrier): EitherT[Future, DataRetrievalError, Option[Boolean]] =
    registration.relevantApRevenue match {
      case Some(revenue) =>
        registration.relevantAp12Months match {
          case Some(true)  => calculateLiabilityAmount(EclTaxYear.YearInDays, revenue)
          case Some(false) =>
            registration.relevantApLength match {
              case Some(relevantApLength) => calculateLiabilityAmount(relevantApLength, revenue)
              case _                      => EitherT.fromEither[Future](Right(None))
            }
          case _           => EitherT.fromEither[Future](Right(None))
        }
      case _             => EitherT.fromEither[Future](Right(None))
    }

  private def calculateLiabilityAmount(relevantApLength: Int, revenue: BigDecimal)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, DataRetrievalError, Option[Boolean]] =
    EitherT {
      eclCalculatorConnector
        .calculateLiability(relevantApLength, revenue)
        .map { liability =>
          val revenueMoreThanZero = liability.amountDue.amount > 0
          Right(Some(revenueMoreThanZero))
        }
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

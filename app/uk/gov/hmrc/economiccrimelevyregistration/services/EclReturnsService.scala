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

import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclReturnsService @Inject() (eclReturnsConnector: EclReturnsConnector)(implicit
  ec: ExecutionContext
) {
  def checkIfRevenueMeetsThreshold(registration: Registration)(implicit hc: HeaderCarrier): Future[Option[Boolean]] =
    registration.relevantApRevenue match {
      case Some(revenue) =>
        val f: Int => Future[Option[Boolean]] = eclReturnsConnector
          .calculateLiability(_, revenue)
          .map(liability =>
            if (liability.amountDue > 0) {
              Some(true)
            } else {
              Some(false)
            }
          )

        registration.relevantAp12Months match {
          case Some(true)  => f(EclTaxYear.YearInDays)
          case Some(false) =>
            registration.relevantApLength match {
              case Some(relevantApLength) => f(relevantApLength)
              case _                      => Future.successful(None)
            }
          case _           => Future.successful(None)
        }

      case _ => Future.successful(None)
    }

}

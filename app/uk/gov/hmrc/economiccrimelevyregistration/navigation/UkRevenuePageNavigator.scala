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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkRevenuePageNavigator @Inject() (
  eclReturnsConnector: EclReturnsConnector
)(implicit ec: ExecutionContext)
    extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {
  override protected def navigateInNormalMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] =
    registration.relevantApRevenue match {
      case Some(revenue) =>
        val f: Int => Future[Call] = eclReturnsConnector
          .calculateLiability(_, revenue)
          .map(liability =>
            if (liability.amountDue > 0) {
              routes.EntityTypeController.onPageLoad(NormalMode)
            } else {
              routes.NotLiableController.onPageLoad()
            }
          )

        // TODO: Can we refactor any of this to remove the duplication for CheckMode?
        // TODO: Do we really mean to go to the start of the journey rather than the recovery page?

        registration.relevantAp12Months match {
          case Some(true)  => f(EclTaxYear.YearInDays)
          case Some(false) =>
            registration.relevantApLength match {
              case Some(relevantApLength) => f(relevantApLength)
              case _                      => Future.successful(routes.StartController.onPageLoad())
            }
          case _           => Future.successful(routes.StartController.onPageLoad())
        }
      case _             => Future.successful(routes.StartController.onPageLoad())
    }

  override protected def navigateInCheckMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] =
    registration.relevantApRevenue match {
      case Some(revenue) =>
        val f: Int => Future[Call] = eclReturnsConnector
          .calculateLiability(_, revenue)
          .map(liability =>
            if (liability.amountDue > 0) {
              routes.CheckYourAnswersController.onPageLoad()
            } else {
              routes.NotLiableController.onPageLoad()
            }
          )

        registration.relevantAp12Months match {
          case Some(true)  => f(EclTaxYear.YearInDays)
          case Some(false) =>
            registration.relevantApLength match {
              case Some(relevantApLength) => f(relevantApLength)
              case _                      => Future.successful(routes.StartController.onPageLoad())
            }
          case _           => Future.successful(routes.StartController.onPageLoad())
        }
      case _             => Future.successful(routes.StartController.onPageLoad())
    }

}

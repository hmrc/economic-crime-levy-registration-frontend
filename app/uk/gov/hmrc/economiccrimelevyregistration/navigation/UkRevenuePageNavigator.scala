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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}
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
  ): Future[Call] = navigate(NormalMode, registration)

  override protected def navigateInCheckMode(registration: Registration)(implicit
    request: RequestHeader
  ): Future[Call] = navigate(CheckMode, registration)

  private def navigate(mode: Mode, registration: Registration)(implicit request: RequestHeader): Future[Call] =
    registration.relevantApRevenue match {
      case Some(revenue) =>
        val f: Int => Future[Call] = eclReturnsConnector
          .calculateLiability(_, revenue)
          .map(liability =>
            if (liability.amountDue > 0) {
              mode match {
                case NormalMode => routes.EntityTypeController.onPageLoad(NormalMode)
                case CheckMode  => routes.CheckYourAnswersController.onPageLoad()
              }
            } else {
              routes.NotLiableController.onPageLoad()
            }
          )

        registration.relevantAp12Months match {
          case Some(true)  => f(EclTaxYear.YearInDays)
          case Some(false) =>
            registration.relevantApLength match {
              case Some(relevantApLength) => f(relevantApLength)
              case _                      => Future.successful(routes.JourneyRecoveryController.onPageLoad())
            }
          case _           => Future.successful(routes.JourneyRecoveryController.onPageLoad())
        }
      case _             => Future.successful(routes.JourneyRecoveryController.onPageLoad())
    }

}

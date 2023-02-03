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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, EclReturnsConnector}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

import scala.concurrent.Future

class UkRevenuePageNavigatorSpec extends SpecBase {

  val mockEclReturnsConnector: EclReturnsConnector           = mock[EclReturnsConnector]
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator = new UkRevenuePageNavigator(mockEclRegistrationConnector, mockEclReturnsConnector)

  "nextPage" should {
    "return a Call to the entity type page in NormalMode when the amount due is more than 0" in forAll {
      (registration: Registration, ukRevenue: Long, calculatedLiability: CalculatedLiability) =>
        val updatedRegistration: Registration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

        when(mockEclReturnsConnector.calculateLiability(any(), any())(any()))
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 1)))

        await(pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)) shouldBe routes.EntityTypeController
          .onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode when the amount due is more than 0" in forAll {
      (registration: Registration, ukRevenue: Long, calculatedLiability: CalculatedLiability) =>
        val updatedRegistration: Registration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

        when(mockEclReturnsConnector.calculateLiability(any(), any())(any()))
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 1)))

        await(
          pageNavigator.nextPage(CheckMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }

    "return a Call to the not liable page in either mode when the amount due is 0" in forAll {
      (registration: Registration, ukRevenue: Long, calculatedLiability: CalculatedLiability, mode: Mode) =>
        val updatedRegistration: Registration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

        when(mockEclRegistrationConnector.deleteRegistration(any())(any())).thenReturn(Future.successful(()))

        when(mockEclReturnsConnector.calculateLiability(any(), any())(any()))
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 0)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe routes.NotLiableController
          .onPageLoad()
    }
  }

}

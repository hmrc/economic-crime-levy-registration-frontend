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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculatedLiability, NormalMode, Registration}

import scala.concurrent.Future

class UkRevenuePageNavigatorSpec extends SpecBase {

  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]

  val pageNavigator = new UkRevenuePageNavigator(mockEclReturnsConnector)

  "nextPage" should {
    "return a Call to the entity type page in NormalMode when the amount due is more than 0" in forAll {
      (registration: Registration, ukRevenue: Long, calculatedLiability: CalculatedLiability) =>
        val updatedRegistration: Registration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

        when(mockEclReturnsConnector.calculateLiability(any(), any())(any()))
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 1)))

        await(pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)) shouldBe routes.EntityTypeController
          .onPageLoad()
    }

    "return a Call to the not liable page in NormalMode when the amount due is 0" in forAll {
      (registration: Registration, ukRevenue: Long, calculatedLiability: CalculatedLiability) =>
        val updatedRegistration: Registration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

        when(mockEclReturnsConnector.calculateLiability(any(), any())(any()))
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 0)))

        await(pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)) shouldBe routes.NotLiableController
          .onPageLoad()
    }
  }

}

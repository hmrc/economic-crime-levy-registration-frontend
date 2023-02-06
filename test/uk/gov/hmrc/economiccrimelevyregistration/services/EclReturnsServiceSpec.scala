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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclReturnsConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculatedLiability, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import scala.concurrent.Future

class EclReturnsServiceSpec extends SpecBase {
  val mockEclReturnsConnector: EclReturnsConnector = mock[EclReturnsConnector]
  val service                                      = new EclReturnsService(mockEclReturnsConnector)

  "checkIfRevenueMeetsThreshold" should {
    "return true if the amount due is greater than 0 and the relevant AP is 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: Long,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclReturnsConnector
            .calculateLiability(ArgumentMatchers.eq(EclTaxYear.YearInDays), ArgumentMatchers.eq(relevantApRevenue))(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 1)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration))

        result shouldBe Some(true)
    }

    "return true if the amount due is greater than 0 and the relevant AP is not 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: Long,
        relevantApLength: Int,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(
            relevantAp12Months = Some(false),
            relevantApLength = Some(relevantApLength),
            relevantApRevenue = Some(relevantApRevenue)
          )

        when(
          mockEclReturnsConnector
            .calculateLiability(ArgumentMatchers.eq(relevantApLength), ArgumentMatchers.eq(relevantApRevenue))(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 1)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration))

        result shouldBe Some(true)
    }

    "return false if the amount due is not greater than 0 and the relevant AP is 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: Long,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclReturnsConnector
            .calculateLiability(ArgumentMatchers.eq(EclTaxYear.YearInDays), ArgumentMatchers.eq(relevantApRevenue))(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 0)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration))

        result shouldBe Some(false)
    }

    "return false if the amount due is not greater than 0 and the relevant AP is not 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: Long,
        relevantApLength: Int,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(
            relevantAp12Months = Some(false),
            relevantApLength = Some(relevantApLength),
            relevantApRevenue = Some(relevantApRevenue)
          )

        when(
          mockEclReturnsConnector
            .calculateLiability(ArgumentMatchers.eq(relevantApLength), ArgumentMatchers.eq(relevantApRevenue))(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = 0)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration))

        result shouldBe Some(false)
    }
  }
}

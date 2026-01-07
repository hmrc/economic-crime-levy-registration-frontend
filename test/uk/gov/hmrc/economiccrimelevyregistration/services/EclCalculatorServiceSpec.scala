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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclCalculatorConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CalculatedLiability, EclAmount, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.http.UpstreamErrorResponse
import org.mockito.Mockito.when

import scala.concurrent.Future

class EclCalculatorServiceSpec extends SpecBase {
  val mockEclCalculatorConnector: EclCalculatorConnector = mock[EclCalculatorConnector]
  val service                                            = new EclCalculatorService(mockEclCalculatorConnector)

  "checkIfRevenueMeetsThreshold" should {
    "return true if the calculated liability amount due is greater than 0 and the relevant AP is 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(EclTaxYear.yearInDays),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = EclAmount(amount = 1))))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(Some(true))
    }

    "return true if the calculated liability amount due is greater than 0 and the relevant AP is not 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal,
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
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(relevantApLength),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = EclAmount(amount = 1))))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(Some(true))
    }

    "return false if the calculated liability amount due is not greater than 0 and the relevant AP is 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal,
        calculatedLiability: CalculatedLiability
      ) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(EclTaxYear.yearInDays),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = EclAmount(amount = 0))))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(Some(false))
    }

    "return false if the calculated liability amount due is not greater than 0 and the relevant AP is not 12 months" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal,
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
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(relevantApLength),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.successful(calculatedLiability.copy(amountDue = EclAmount(amount = 0))))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(Some(false))
    }

    "return None if the relevantApRevenue is set to None " in forAll { (registration: Registration) =>
      val updatedRegistration =
        registration.copy(
          relevantApRevenue = None
        )

      val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

      result shouldBe Right(None)
    }

    "return None if the relevantAp12Months is set to None when the relevantApRevenue contains a value " in forAll {
      (registration: Registration, relevantApRevenue: BigDecimal) =>
        val updatedRegistration =
          registration.copy(
            relevantAp12Months = None,
            relevantApRevenue = Some(relevantApRevenue)
          )

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(None)
    }

    "return None if the relevantApLength is set to None when relevantApRevenue contains a value and relevantAp12Months is set to false" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal
      ) =>
        val updatedRegistration =
          registration.copy(
            relevantAp12Months = Some(false),
            relevantApLength = None,
            relevantApRevenue = Some(relevantApRevenue)
          )

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Right(None)
    }

    "return an error when the call to the calculator connector returns an Upstream5xxResponse" in forAll {
      (
        registration: Registration,
        relevantApRevenue: BigDecimal
      ) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(EclTaxYear.yearInDays),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return an error when the call to the calculator connector returns an Upstream4xxResponse" in forAll {
      (registration: Registration, relevantApRevenue: BigDecimal) =>
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(EclTaxYear.yearInDays),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Error", NOT_FOUND)))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Left(DataRetrievalError.BadGateway("Error", NOT_FOUND))
    }

    "return an error if an exception is thrown" in forAll {
      (registration: Registration, relevantApRevenue: BigDecimal) =>
        val exception           = new Exception("error")
        val updatedRegistration =
          registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(relevantApRevenue))

        when(
          mockEclCalculatorConnector
            .calculateLiability(
              ArgumentMatchers.eq(EclTaxYear.yearInDays),
              ArgumentMatchers.eq(relevantApRevenue),
              ArgumentMatchers.eq(EclTaxYear.fromCurrentDate().startYear)
            )(
              any()
            )
        )
          .thenReturn(Future.failed(exception))

        val result = await(service.checkIfRevenueMeetsThreshold(updatedRegistration).value)

        result shouldBe Left(DataRetrievalError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }
}

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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{Bands, CalculateLiabilityRequest, CalculatedLiability, EclAmount}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class EclCalculatorConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EclCalculatorConnector(appConfig, mockHttpClient)
  val eclCalculatorUrl                   = "http://localhost:14010/economic-crime-levy-calculator"
  val expectedUrl                        = url"$eclCalculatorUrl/calculate-liability"

  "calculateLiability" should {
    "return the calculated liability when the http client returns the calculated liability" in forAll {
      (calculateLiabilityRequest: CalculateLiabilityRequest, calculatedLiability: CalculatedLiability) =>
        val newCalculatedLiabilityRequest =
          calculateLiabilityRequest.copy(amlRegulatedActivityLength = EclTaxYear.yearInDays)
        val bandRange                     = calculatedLiability.bands.small.copy(amount = BigDecimal(20000.00))
        val band                          = Bands(small = bandRange, medium = bandRange, large = bandRange, veryLarge = bandRange)
        val newCalculatedLiability        = calculatedLiability.copy(amountDue = EclAmount(BigDecimal(2000.00)), bands = band)

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder
            .withBody(ArgumentMatchers.eq(Json.toJson(newCalculatedLiabilityRequest)))(any(), any(), any())
        ).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.toJson(newCalculatedLiability).toString())))

        val result = await(
          connector.calculateLiability(
            calculateLiabilityRequest.relevantApLength,
            calculateLiabilityRequest.ukRevenue,
            calculateLiabilityRequest.year
          )
        )

        result shouldBe newCalculatedLiability

    }
  }

}

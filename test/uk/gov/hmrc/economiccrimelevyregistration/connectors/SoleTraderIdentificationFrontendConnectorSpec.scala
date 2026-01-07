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
import org.scalacheck.{Arbitrary, Gen}
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class SoleTraderIdentificationFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new SoleTraderIdentificationFrontendConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val apiUrl                             = s"${appConfig.soleTraderEntityIdentificationFrontendBaseUrl}/sole-trader-identification/api"

  "createSoleTraderJourney" should {
    "return a GRS create journey response for the given request when the http client returns a GRS create journey response for the given request" in forAll {
      (grsCreateJourneyResponse: GrsCreateJourneyResponse, mode: Mode) =>
        val expectedUrl = s"$apiUrl/sole-trader-journey"

        val expectedSoleTraderEntityCreateJourneyRequest: SoleTraderEntityCreateJourneyRequest = {
          val serviceNameLabels = ServiceNameLabels(
            OptServiceName("Register for the Economic Crime Levy"),
            OptServiceName("Cofrestru ar gyfer yr Ardoll Troseddau Economaidd")
          )

          SoleTraderEntityCreateJourneyRequest(
            continueUrl =
              s"http://localhost:14000/register-for-economic-crime-levy/grs-continue/${mode.toString.toLowerCase}",
            businessVerificationCheck = false,
            optServiceName = Some(serviceNameLabels.en.optServiceName),
            deskProServiceId = "economic-crime-levy-registration-frontend",
            signOutUrl = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
            accessibilityUrl = "/accessibility-statement/economic-crime-levy",
            labels = serviceNameLabels
          )
        }

        when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedSoleTraderEntityCreateJourneyRequest)))(
            any(),
            any(),
            any()
          )
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(grsCreateJourneyResponse)))))

        val result = await(connector.createSoleTraderJourney(mode))

        result shouldBe grsCreateJourneyResponse

    }
  }

  "getJourneyData" should {
    "return journey data for a given journey id" in forAll(
      Arbitrary.arbitrary[SoleTraderEntityJourneyData],
      Gen.uuid.map(_.toString)
    ) { (soleTraderEntityJourneyData, journeyId) =>
      val expectedUrl = s"$apiUrl/journey/$journeyId"

      when(mockHttpClient.get(ArgumentMatchers.eq(url"$expectedUrl"))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(
          Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(soleTraderEntityJourneyData))))
        )

      val result = await(connector.getJourneyData(journeyId))

      result shouldBe soleTraderEntityJourneyData
    }
  }
}

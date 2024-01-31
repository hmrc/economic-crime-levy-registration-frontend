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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status._
import play.api.http.HeaderNames._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class AddressLookupFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder           = mock[RequestBuilder]
  val connector                    = new AddressLookupFrontendConnectorImpl(appConfig, mockHttpClient)
  val baseUrl                      = appConfig.addressLookupFrontendBaseUrl

  //when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
  //          .thenReturn(mockRequestBuilder)
  //        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
  //          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(obligationData)))))

  //when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
  //        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
  //          .thenReturn(mockRequestBuilder)
  //        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
  //          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(calculatedLiability)))))

  "initJourney" should {
    val expectedUrl = s"$baseUrl/api/init"
    val ukMode      = random[Boolean]
    val alfLabels   = AlfEnCyLabels(appConfig)

    val expectedJourneyConfig: AlfJourneyConfig =
      AlfJourneyConfig(
        options = AlfOptions(
          continueUrl = "http://localhost:14000/register-for-economic-crime-levy/address-lookup-continue/normalmode",
          homeNavHref = "/register-for-economic-crime-levy",
          signOutHref = "http://localhost:14000/register-for-economic-crime-levy/account/sign-out-survey",
          accessibilityFooterUrl = "/accessibility-statement/economic-crime-levy",
          deskProServiceName = "economic-crime-levy-registration-frontend",
          ukMode = ukMode
        ),
        labels = alfLabels
      )

    "return the url for the address lookup journey in the Location header when the http client returns a 202 response" in forAll {
      (journeyUrl: String) =>
        val headers  = Map(LOCATION -> Seq(journeyUrl))
        val response = HttpResponse(ACCEPTED, "", headers)

        when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = await(connector.initJourney(ukMode, NormalMode))

        result shouldBe journeyUrl

    }

    "throw an IllegalStateException when the http client returns a 202 response but the Location header is missing" in {
      val response = HttpResponse(ACCEPTED, "", Map.empty)

      when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result: IllegalStateException = intercept[IllegalStateException] {
        await(connector.initJourney(ukMode, NormalMode))
      }

      result.getMessage shouldBe "Location header not present in response"

    }

    "throw an exception when the http client returns a response other than 202" in {
      val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

      when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.failed(response))

      val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(connector.initJourney(ukMode, NormalMode)) //UNSURE ON THIS ONE
      }

      result.getMessage shouldBe "Internal server error"

    }
  }

  "getAddress" should {
    "return the address identified by the given journey id" in forAll {
      (journeyId: String, alfAddressData: AlfAddressData) =>
        val expectedUrl         = s"$baseUrl/api/confirmed?"
        val expectedQueryParams = Seq(("id", journeyId))

        when(mockHttpClient.get(ArgumentMatchers.eq(url"$expectedUrl"))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(alfAddressData)))))

        val result = await(connector.getAddress(journeyId))

        result shouldBe alfAddressData

//        verify(mockHttpClient, times(1))
//          .GET[AlfAddressData](
//            ArgumentMatchers.eq(expectedUrl),
//            ArgumentMatchers.eq(expectedQueryParams),
//            any()
//          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}

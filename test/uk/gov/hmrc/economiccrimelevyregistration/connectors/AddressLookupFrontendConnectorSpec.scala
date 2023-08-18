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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.{HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class AddressLookupFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new AddressLookupFrontendConnectorImpl(appConfig, mockHttpClient)
  val baseUrl                    = appConfig.addressLookupFrontendBaseUrl

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

        when(
          mockHttpClient.POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedJourneyConfig),
            any()
          )(any(), any(), any(), any())
        )
          .thenReturn(Future.successful(Right(response)))

        val result = await(connector.initJourney(ukMode, NormalMode))

        result shouldBe journeyUrl

        verify(mockHttpClient, times(1))
          .POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedJourneyConfig),
            any()
          )(any(), any(), any(), any())

        reset(mockHttpClient)
    }

    "throw an IllegalStateException when the http client returns a 202 response but the Location header is missing" in {
      val response = HttpResponse(ACCEPTED, "", Map.empty)

      when(
        mockHttpClient.POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedJourneyConfig),
          any()
        )(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(Right(response)))

      val result: IllegalStateException = intercept[IllegalStateException] {
        await(connector.initJourney(ukMode, NormalMode))
      }

      result.getMessage shouldBe "Location header not present in response"

      verify(mockHttpClient, times(1))
        .POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedJourneyConfig),
          any()
        )(any(), any(), any(), any())

      reset(mockHttpClient)
    }

    "throw an exception when the http client returns a response other than 202" in {
      val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

      when(
        mockHttpClient.POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedJourneyConfig),
          any()
        )(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(Left(response)))

      val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(connector.initJourney(ukMode, NormalMode))
      }

      result.getMessage shouldBe "Internal server error"

      verify(mockHttpClient, times(1))
        .POST[AlfJourneyConfig, Either[UpstreamErrorResponse, HttpResponse]](
          ArgumentMatchers.eq(expectedUrl),
          ArgumentMatchers.eq(expectedJourneyConfig),
          any()
        )(any(), any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "getAddress" should {
    "return the address identified by the given journey id" in forAll {
      (journeyId: String, alfAddressData: AlfAddressData) =>
        val expectedUrl         = s"$baseUrl/api/confirmed"
        val expectedQueryParams = Seq(("id", journeyId))

        when(
          mockHttpClient.GET[AlfAddressData](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedQueryParams),
            any()
          )(any(), any(), any())
        )
          .thenReturn(Future.successful(alfAddressData))

        val result = await(connector.getAddress(journeyId))

        result shouldBe alfAddressData

        verify(mockHttpClient, times(1))
          .GET[AlfAddressData](
            ArgumentMatchers.eq(expectedUrl),
            ArgumentMatchers.eq(expectedQueryParams),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}

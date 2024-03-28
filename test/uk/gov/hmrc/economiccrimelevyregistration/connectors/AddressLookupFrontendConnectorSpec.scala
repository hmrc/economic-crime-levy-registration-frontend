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
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AddressLookupFrontendConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new AddressLookupFrontendConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val baseUrl: String                    = appConfig.addressLookupFrontendBaseUrl

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

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

    "return the url for the address lookup journey in the Location header when the http client returns a 202 response" in {
      beforeEach()
      val headers  = Map(LOCATION -> Seq("journeyUrl"))
      val response = HttpResponse(ACCEPTED, "", headers)

      when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result = await(connector.initJourney(ukMode, NormalMode))

      result shouldBe "journeyUrl"

    }

    "throw an IllegalStateException when the http client returns a 202 response but the Location header is missing" in {
      beforeEach()

      val headers  = Map(LOCATION -> Seq.empty)
      val response = HttpResponse(ACCEPTED, "No location header found", headers)

      when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      Try(await(connector.initJourney(ukMode, NormalMode))) match {
        case Failure(thr) => thr.getMessage shouldBe "No location header found"
        case Success(_)   => fail("expected exception to be thrown")
      }

    }

    "throw an exception when the http client returns a response other than 202" in {
      val response = HttpResponse(INTERNAL_SERVER_ERROR, "Internal server error")
      beforeEach()

      when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(Json.toJson(expectedJourneyConfig)))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
        await(connector.initJourney(ukMode, NormalMode))
      }

      result.getMessage shouldBe "Internal server error"

    }
  }

  "getAddress" should {
    "return the address identified by the given journey id" in forAll(
      Gen.uuid.map(_.toString),
      Arbitrary.arbitrary[AlfAddressData]
    ) { (journeyId, alfAddressData) =>
      beforeEach()
      val expectedUrl = s"$baseUrl/api/confirmed?id=$journeyId"

      when(mockHttpClient.get(ArgumentMatchers.eq(url"$expectedUrl"))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(alfAddressData)))))

      val result = await(connector.getAddress(journeyId))

      result shouldBe alfAddressData

    }
  }
}

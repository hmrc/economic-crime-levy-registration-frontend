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
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CreateEclSubscriptionResponse, EclSubscriptionStatus, Registration}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class EclRegistrationConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EclRegistrationConnector(appConfig, mockHttpClient)
  val eclRegistrationUrl                 = "http://localhost:14001/economic-crime-levy-registration"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "getRegistration" should {

    "return a registration when the http client returns a registration" in forAll {
      (internalId: String, registration: Registration, authHeader: (String, String)) =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId"
        val response    = HttpResponse(ACCEPTED, Json.toJson(registration).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(ArgumentMatchers.eq("Authorization" -> hc.authorization.get.value)))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getRegistration(internalId))

        result shouldBe registration

    }

  }

  "deleteRegistration" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      beforeEach()
      val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId"
      val response    = HttpResponse(NO_CONTENT, "")

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result: Unit = await(connector.deleteRegistration(internalId))
      result shouldBe ()

    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId"
        val response    = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.failed(response))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.deleteRegistration(internalId))
        }

        result.getMessage shouldBe "Internal server error"
    }
  }

  "upsertRegistration" should {
    "return a unit when registration is successfully upserted" in forAll { registration: Registration =>
      beforeEach()
      val expectedUrl = url"$eclRegistrationUrl/registrations"
      val response    = HttpResponse(NO_CONTENT, "")

      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(Json.toJson(registration))).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

      val result = await(connector.upsertRegistration(registration))
      result shouldBe ()

    }
  }

  "getSubscriptionStatus" should {
    "return an EclSubscriptionStatus when the http client returns an EclSubscriptionStatus" in forAll {
      (businessPartnerId: String, eclSubscriptionStatus: EclSubscriptionStatus) =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/subscription-status/$businessPartnerId"
        val response    = HttpResponse(OK, Json.toJson(eclSubscriptionStatus).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getSubscriptionStatus(businessPartnerId))

        result shouldBe eclSubscriptionStatus

    }
  }

  "getRegistrationValidationErrors" should {
    "return None when the http client returns 204 no content with no validation errors" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"
        val response    = HttpResponse(NO_CONTENT, "")

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getRegistrationValidationErrors(internalId))

        result shouldBe None
    }

    "return validation error when the http client return 200 ok with validation error" in forAll {
      (internalId: String, dataValidationError: DataValidationError) =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"
        val response    = HttpResponse(OK, Json.toJson(dataValidationError)(any()), Map.empty)

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getRegistrationValidationErrors(internalId))

        result shouldBe Some(dataValidationError)
    }

//    "throw a HttpException when an unexpected http status is returned by the http client" in forAll {
//      internalId: String =>
//        beforeEach()
//        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"
//
//        val response = HttpResponse(ACCEPTED, "")
//
//        when(
//          mockHttpClient
//            .GET[Either[UpstreamErrorResponse, HttpResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(
//              any(),
//              any(),
//              any()
//            )
//        ).thenReturn(Future.successful(Right(response)))
//
//        val result: HttpException = intercept[HttpException] {
//          await(connector.getRegistrationValidationErrors(internalId))
//        }
//
//        result.getMessage shouldBe s"Unexpected response with HTTP status $ACCEPTED"
//    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.failed(response))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.getRegistrationValidationErrors(internalId))
        }

        result.getMessage shouldBe "Internal server error"
    }
  }

  "submitRegistration" should {
    "return a subscription response when the http client returns a subscription response" in forAll {
      (internalId: String, subscriptionResponse: CreateEclSubscriptionResponse) =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/submit-registration/$internalId"
        val response    = HttpResponse(OK, Json.toJson(subscriptionResponse)(any()).toString(), Map.empty)

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(ArgumentMatchers.eq("Authorization" -> hc.authorization.get.value)))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = await(connector.submitRegistration(internalId))

        result shouldBe subscriptionResponse

    }
  }
}

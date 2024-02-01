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
import org.scalacheck.Arbitrary
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CreateEclSubscriptionResponse, EclSubscriptionStatus, Registration}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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
      (internalId: String, registration: Registration, authorization: Authorization) =>
        beforeEach()
        val bigDecimal          = BigDecimal(20000000.00)
        val updatedRegistration = registration.copy(relevantApRevenue = Some(bigDecimal))
        val hc: HeaderCarrier   = HeaderCarrier(Some(authorization))
        val expectedUrl         = url"$eclRegistrationUrl/registrations/$internalId"
        val response            = HttpResponse(ACCEPTED, Json.toJson(updatedRegistration).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(ArgumentMatchers.eq("Authorization" -> hc.authorization.get.value)))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getRegistration(internalId)(hc))

        result shouldBe updatedRegistration

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
        val msg         = "Internal server error"

        when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.deleteRegistration(internalId))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
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
        val response    = HttpResponse(NO_CONTENT, Json.toJson(None).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getRegistrationValidationErrors(internalId).value)

        result shouldBe None
    }

    "return Some with DataValidationError when 200 OK is returned with validation error in the body" in forAll {
      (internalId: String, dataValidationError: String) =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, (Json.toJson(dataValidationError).toString()))))

        val result = await(connector.getRegistrationValidationErrors(internalId).value)

        result shouldBe Some(dataValidationError)
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$eclRegistrationUrl/registrations/$internalId/validation-errors"
        val msg         = "Internal server error"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.getRegistrationValidationErrors(internalId).value)) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }

  "submitRegistration" should {
    "return a subscription response when the http client returns a subscription response" in forAll {
      (internalId: String, subscriptionResponse: CreateEclSubscriptionResponse, authorization: Authorization) =>
        beforeEach()
        val hc: HeaderCarrier = HeaderCarrier(Some(authorization))
        val expectedUrl       = url"$eclRegistrationUrl/submit-registration/$internalId"
        val response          = HttpResponse(OK, Json.toJson(subscriptionResponse).toString())

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(ArgumentMatchers.eq("Authorization" -> hc.authorization.get.value)))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(response))

        val result = await(connector.submitRegistration(internalId)(hc))

        result shouldBe subscriptionResponse

    }
  }
}

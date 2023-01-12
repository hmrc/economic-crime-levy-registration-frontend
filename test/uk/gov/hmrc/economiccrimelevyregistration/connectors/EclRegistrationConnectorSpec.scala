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
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, Registration}
import uk.gov.hmrc.http.{HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class EclRegistrationConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclRegistrationConnector(appConfig, mockHttpClient)
  val eclRegistrationUrl         = "http://localhost:14001/economic-crime-levy-registration"

  "getRegistration" should {

    "return a registration when the http client returns a registration" in forAll {
      (internalId: String, registration: Registration) =>
        val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId"

        when(
          mockHttpClient.GET[Option[Registration]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
        )
          .thenReturn(Future.successful(Some(registration)))

        val result = await(connector.getRegistration(internalId))

        result shouldBe Some(registration)

        verify(mockHttpClient, times(1))
          .GET[Option[Registration]](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }

    "return none when the http client returns none" in forAll { internalId: String =>
      val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId"

      when(
        mockHttpClient.GET[Option[Registration]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
      )
        .thenReturn(Future.successful(None))

      val result = await(connector.getRegistration(internalId))
      result shouldBe None

      verify(mockHttpClient, times(1))
        .GET[Option[Registration]](
          ArgumentMatchers.eq(expectedUrl),
          any(),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "deleteRegistration" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId"

      val response = HttpResponse(NO_CONTENT, "", Map.empty)

      when(mockHttpClient.DELETE[HttpResponse](ArgumentMatchers.eq(expectedUrl), any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result: Unit = await(connector.deleteRegistration(internalId))
      result shouldBe ()

      verify(mockHttpClient, times(1))
        .DELETE[HttpResponse](
          ArgumentMatchers.eq(expectedUrl),
          any()
        )(any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "upsertRegistration" should {
    "return the new or updated registration" in forAll { registration: Registration =>
      val expectedUrl = s"$eclRegistrationUrl/registrations"

      when(
        mockHttpClient
          .PUT[Registration, Registration](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(registration))

      val result = await(connector.upsertRegistration(registration))
      result shouldBe registration

      verify(mockHttpClient, times(1))
        .PUT[Registration, Registration](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())

      reset(mockHttpClient)
    }
  }

  "getSubscriptionStatus" should {
    "return an EclSubscriptionStatus when the http client returns an EclSubscriptionStatus" in forAll {
      (businessPartnerId: String, eclSubscriptionStatus: EclSubscriptionStatus) =>
        val expectedUrl = s"$eclRegistrationUrl/subscription-status/$businessPartnerId"

        when(
          mockHttpClient.GET[EclSubscriptionStatus](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
        )
          .thenReturn(Future.successful(eclSubscriptionStatus))

        val result = await(connector.getSubscriptionStatus(businessPartnerId))

        result shouldBe eclSubscriptionStatus

        verify(mockHttpClient, times(1))
          .GET[EclSubscriptionStatus](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }

  "getRegistrationValidationErrors" should {
    "return None when the http client returns 204 no content with no validation errors" in forAll {
      internalId: String =>
        val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        val response = HttpResponse(NO_CONTENT, "")

        when(
          mockHttpClient
            .GET[Either[UpstreamErrorResponse, HttpResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(Right(response)))

        val result = await(connector.getRegistrationValidationErrors(internalId))

        result shouldBe None
    }

    "return validation errors when the http client return 200 ok with validation errors" in forAll {
      (internalId: String, dataValidationErrors: DataValidationErrors) =>
        val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        val response = HttpResponse(OK, Json.toJson(dataValidationErrors), Map.empty)

        when(
          mockHttpClient
            .GET[Either[UpstreamErrorResponse, HttpResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(Right(response)))

        val result = await(connector.getRegistrationValidationErrors(internalId))

        result shouldBe Some(dataValidationErrors)
    }

    "throw a HttpException when an unexpected http status is returned by the http client" in forAll {
      internalId: String =>
        val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        val response = HttpResponse(ACCEPTED, "")

        when(
          mockHttpClient
            .GET[Either[UpstreamErrorResponse, HttpResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(Right(response)))

        val result: HttpException = intercept[HttpException] {
          await(connector.getRegistrationValidationErrors(internalId))
        }

        result.getMessage shouldBe s"Unexpected response with HTTP status $ACCEPTED"
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        val expectedUrl = s"$eclRegistrationUrl/registrations/$internalId/validation-errors"

        val response = UpstreamErrorResponse("Internal server error", INTERNAL_SERVER_ERROR)

        when(
          mockHttpClient
            .GET[Either[UpstreamErrorResponse, HttpResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(
              any(),
              any(),
              any()
            )
        ).thenReturn(Future.successful(Left(response)))

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.getRegistrationValidationErrors(internalId))
        }

        result.getMessage shouldBe "Internal server error"
    }
  }
}

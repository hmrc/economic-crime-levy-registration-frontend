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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class SessionDataConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new SessionDataConnector(appConfig, mockHttpClient)
  val eclSessionDataUrl                  = url"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  override def beforeEach(): Unit = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "get" should {
    "return SessionData data when request succeeds" in forAll { (internalId: String, sessionData: SessionData) =>
      beforeEach()
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"

      when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(sessionData)))))

      val result = await(connector.get(internalId))

      result shouldBe sessionData
    }

    "throw an UpstreamErrorResponse exception when the http client returns an error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$eclSessionDataUrl/session/$internalId"
        val msg         = "Internal server error"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.get(internalId))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }

  "delete" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      beforeEach()
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, "")))

      val result: Unit = await(connector.delete(internalId))
      result shouldBe ()
    }

    "return an UpstreamErrorResponse when the http client returns an error response" in forAll { internalId: String =>
      beforeEach()
      val expectedUrl = url"$eclSessionDataUrl/session/$internalId"
      val msg         = "Internal server error"

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, msg)))

      Try(await(connector.delete(internalId))) match {
        case Failure(thr) => thr.getMessage shouldBe msg
        case Success(a)   => fail("expected exception to be thrown" + a)
      }
    }
  }

  "upsert" should {
    val expectedUrl = url"$eclSessionDataUrl/session"
    "return unit when request succeeds" in forAll { sessionData: SessionData =>
      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(NO_CONTENT, "")))

      val result: Unit = await(connector.upsert(sessionData))
      result shouldBe ()
    }

    "return an UpstreamErrorResponse when the http client returns an error response" in forAll {
      sessionData: SessionData =>
        val expectedUrl = url"$eclSessionDataUrl/session"
        val msg         = "Internal server error"

        when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.upsert(sessionData))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }

}

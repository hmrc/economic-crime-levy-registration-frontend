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

package uk.gov.hmrc.economiccrimelevyregistration.connectors.deregister

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class DeregistrationConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new DeregistrationConnector(appConfig, mockHttpClient)
  val deregistrationUrl                  = "http://localhost:14001/economic-crime-levy-registration"

  override def beforeEach() = {
    reset(mockHttpClient)
    reset(mockRequestBuilder)
  }

  "getDeregistration" should {
    "return a registration when the http client returns a registration" in forAll {
      (deregistration: Deregistration, authorization: Authorization) =>
        beforeEach()
        val hc: HeaderCarrier = HeaderCarrier(Some(authorization))
        val expectedUrl       = url"$deregistrationUrl/deregistration/${deregistration.internalId}"
        val response          = HttpResponse(ACCEPTED, Json.toJson(deregistration).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(ArgumentMatchers.eq("Authorization" -> hc.authorization.get.value)))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getDeregistration(deregistration.internalId)(hc))

        result shouldBe deregistration
    }
  }

  "deleteDeregistration" should {
    "return unit when the http client successfully returns a http response" in forAll { internalId: String =>
      beforeEach()
      val expectedUrl = url"$deregistrationUrl/deregistration/$internalId"
      val response    = HttpResponse(NO_CONTENT, "")

      when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result: Unit = await(connector.deleteDeregistration(internalId))
      result shouldBe ()
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$deregistrationUrl/deregistration/$internalId"
        val msg         = "Internal server error"

        when(mockHttpClient.delete(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.deleteDeregistration(internalId))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }

  "upsertDeregistration" should {
    "return a unit when registration is successfully upserted" in forAll { deregistration: Deregistration =>
      beforeEach()
      val expectedUrl = url"$deregistrationUrl/deregistration"
      val response    = HttpResponse(NO_CONTENT, "")

      when(mockHttpClient.put(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(Json.toJson(deregistration))).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

      val result = await(connector.upsertDeregistration(deregistration))
      result shouldBe ()
    }
  }

  "submitDeregistration" should {
    "return unit when the submission succeeds" in forAll { (internalId: String, authorization: Authorization) =>
      beforeEach()
      val hc: HeaderCarrier = HeaderCarrier(Some(authorization))
      val expectedUrl       = url"$deregistrationUrl/submit-deregistration/$internalId"
      val response          = HttpResponse(OK, "")

      when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(response))

      val result = await(connector.submitDeregistration(internalId)(hc))

      result shouldBe ()
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      internalId: String =>
        beforeEach()
        val expectedUrl = url"$deregistrationUrl/submit-deregistration/$internalId"
        val msg         = "Internal server error"

        when(mockHttpClient.post(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.submitDeregistration(internalId)(hc))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }
}

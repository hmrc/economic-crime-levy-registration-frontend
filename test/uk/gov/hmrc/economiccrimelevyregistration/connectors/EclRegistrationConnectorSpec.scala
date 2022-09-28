/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class EclRegistrationConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclRegistrationConnector(appConfig, mockHttpClient)
  val eclRegistrationsUrl        = "http://localhost:14001/economic-crime-levy-registration/registrations"

  override def afterEach(): Unit =
    reset(mockHttpClient)

  "getRegistration" should {
    val expectedUrl = s"$eclRegistrationsUrl/$internalId"

    "return a registration when the http client returns a registration" in {
      when(
        mockHttpClient.GET[Option[Registration]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
      )
        .thenReturn(Future.successful(Some(emptyRegistration)))

      val result = await(connector.getRegistration(internalId))

      result shouldBe Some(emptyRegistration)

      verify(mockHttpClient, times(1))
        .GET[Option[Registration]](
          ArgumentMatchers.eq(expectedUrl),
          any(),
          any()
        )(any(), any(), any())
    }

    "return none when the http client returns none" in {
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
    }
  }

  "deleteRegistration" should {
    "return unit when the http client successfully returns a http response" in {
      val expectedUrl = s"$eclRegistrationsUrl/$internalId"

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
    }
  }

  "upsertRegistration" should {
    "return the new or updated registration" in {
      val expectedUrl = eclRegistrationsUrl

      when(
        mockHttpClient
          .PUT[Registration, Registration](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
      )
        .thenReturn(Future.successful(emptyRegistration))

      val result = await(connector.upsertRegistration(emptyRegistration))
      result shouldBe emptyRegistration

      verify(mockHttpClient, times(1))
        .PUT[Registration, Registration](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any(), any())
    }
  }
}
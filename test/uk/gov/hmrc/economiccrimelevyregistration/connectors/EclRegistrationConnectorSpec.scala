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

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class EclRegistrationConnectorSpec extends SpecBase {
  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EclRegistrationConnector(appConfig, mockHttpClient)

  "getRegistration" should {
    "return a registration when the http client returns a registration" in {
      when(mockHttpClient.GET[Option[Registration]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(emptyRegistration)))

      val result = await(connector.getRegistration(internalId))
      result shouldBe Some(emptyRegistration)
    }

    "return none when the http client returns none" in {
      when(mockHttpClient.GET[Option[Registration]](any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(connector.getRegistration(internalId))
      result shouldBe None
    }
  }

  "deleteRegistration" should {
    "return unit when the http client successfully returns a http response" in {
      val response = HttpResponse(NO_CONTENT, "", Map.empty)

      when(mockHttpClient.DELETE[HttpResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(response))

      val result = await(connector.deleteRegistration(internalId))
      result shouldBe ()
    }
  }

  "upsertRegistration" should {
    "return the new or updated registration" in {
      when(mockHttpClient.PUT[Registration, Registration](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(emptyRegistration))

      val result = await(connector.upsertRegistration(emptyRegistration))
      result shouldBe emptyRegistration
    }
  }
}

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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import uk.gov.hmrc.http.{HttpClient, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val enrolmentStoreUrl: String          = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  "getEnrolmentsForGroup" should {
    "return a list of enrolments for the specified group when the http client returns a list of enrolments" in forAll {
      (groupId: String, groupEnrolments: GroupEnrolmentsResponse) =>
        val expectedUrl = url"$enrolmentStoreUrl/groups/$groupId/enrolments"
        val response    = HttpResponse(OK, Json.toJson(groupEnrolments), Map.empty)

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getEnrolmentsForGroup(groupId))

        result shouldBe groupEnrolments
    }
  }
}

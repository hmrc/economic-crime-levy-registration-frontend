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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new EnrolmentStoreProxyConnector(appConfig, mockHttpClient)
  val enrolmentStoreUrl: String  = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store"

  "getEnrolmentsForGroup" should {

    "return a list of enrolments for the specified group when the http client returns a list of enrolments" in forAll {
      (groupId: String, groupEnrolments: Option[GroupEnrolmentsResponse]) =>
        val expectedUrl = s"$enrolmentStoreUrl/groups/$groupId/enrolments"
        when(
          mockHttpClient
            .GET[Option[GroupEnrolmentsResponse]](ArgumentMatchers.eq(expectedUrl), any(), any())(any(), any(), any())
        )
          .thenReturn(Future.successful(groupEnrolments))

        val result = await(connector.getEnrolmentsForGroup(groupId))

        result shouldBe groupEnrolments

        verify(mockHttpClient, times(1))
          .GET[Option[GroupEnrolmentsResponse]](
            ArgumentMatchers.eq(expectedUrl),
            any(),
            any()
          )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}

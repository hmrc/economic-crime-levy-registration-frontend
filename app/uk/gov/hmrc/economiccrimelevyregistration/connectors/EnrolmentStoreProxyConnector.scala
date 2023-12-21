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

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.GroupEnrolmentsResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait EnrolmentStoreProxyConnector {
  def getEnrolmentsForGroup(groupId: String)(implicit hc: HeaderCarrier): Future[GroupEnrolmentsResponse]
}

class EnrolmentStoreProxyConnectorImpl @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends EnrolmentStoreProxyConnector
    with BaseConnector {

  private val enrolmentStoreUrl: URL =
    url"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  def getEnrolmentsForGroup(groupId: String)(implicit hc: HeaderCarrier): Future[GroupEnrolmentsResponse] =
    httpClient
      .get(url"$enrolmentStoreUrl/groups/$groupId/enrolments")
      .executeAndDeserialise[GroupEnrolmentsResponse]
}

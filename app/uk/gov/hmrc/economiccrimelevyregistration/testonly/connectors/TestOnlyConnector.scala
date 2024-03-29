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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val ec: ExecutionContext
) {

  private val eclRegistrationsUrl: String =
    s"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration/test-only"

  def clearAllData()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.GET(
      s"$eclRegistrationsUrl/clear-all"
    )

  def clearCurrentData()(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.GET(
      s"$eclRegistrationsUrl/clear-current"
    )

  def deEnrol(groupId: String, eclReference: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClient.GET(
      s"$eclRegistrationsUrl/de-enrol/$groupId/$eclReference"
    )

}

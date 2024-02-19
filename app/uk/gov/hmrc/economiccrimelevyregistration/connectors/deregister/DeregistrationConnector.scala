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

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.BaseConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregistrationConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends BaseConnector {

  private val deregistrationUrl: URL =
    url"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def getDeregistration(internalId: String)(implicit hc: HeaderCarrier): Future[Deregistration] =
    httpClient
      .get(url"$deregistrationUrl/deregistration/$internalId")
      .executeAndDeserialise[Deregistration]

  def upsertDeregistration(deregistration: Deregistration)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .put(url"$deregistrationUrl/deregistration")
      .withBody(Json.toJson(deregistration))
      .executeAndContinue

  def deleteDeregistration(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .delete(url"$deregistrationUrl/deregistration/$internalId")
      .executeAndContinue

}

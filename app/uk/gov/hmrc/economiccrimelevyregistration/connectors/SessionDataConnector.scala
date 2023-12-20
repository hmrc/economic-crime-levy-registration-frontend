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

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends BaseConnector {

  private val eclRegistrationUrl: URL =
    url"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def get(internalId: String)(implicit hc: HeaderCarrier): Future[SessionData] =
    httpClient
      .get(url"$eclRegistrationUrl/session/$internalId")
      .executeAndDeserialise[SessionData]

  def upsert(session: SessionData)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .put(url"$eclRegistrationUrl/session")
      .withBody(Json.toJson(session))
      .executeAndContinue

  def delete(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.delete(url"$eclRegistrationUrl/session/$internalId").executeAndContinue
}

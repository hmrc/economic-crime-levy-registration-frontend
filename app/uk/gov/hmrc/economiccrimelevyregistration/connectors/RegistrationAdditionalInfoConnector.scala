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
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationAdditionalInfo
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationAdditionalInfoConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit
  ec: ExecutionContext
) extends BaseConnector {

  private val eclRegistrationUrl: URL =
    url"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def get(internalId: String)(implicit hc: HeaderCarrier): Future[RegistrationAdditionalInfo] =
    httpClient
      .get(url"$eclRegistrationUrl/registration-additional-info/$internalId")
      .executeAndDeserialise[RegistrationAdditionalInfo]

  def upsert(registration: RegistrationAdditionalInfo)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .put(url"$eclRegistrationUrl/registration-additional-info")
      .withBody(Json.toJson(registration))
      .executeAndContinue

  def delete(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .delete(url"$eclRegistrationUrl/registration-additional-info/$internalId")
      .executeAndContinue
}

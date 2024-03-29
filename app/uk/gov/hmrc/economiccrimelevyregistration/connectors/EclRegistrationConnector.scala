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

import cats.data.OptionT
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.economiccrimelevyregistration.models.{CreateEclSubscriptionResponse, EclSubscriptionStatus, GetSubscriptionResponse, Registration}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclRegistrationConnector @Inject() (appConfig: AppConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext)
    extends BaseConnector {

  private val eclRegistrationUrl: URL =
    url"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def getRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Registration] = {
    val request = httpClient
      .get(url"$eclRegistrationUrl/registrations/$internalId")

    hc.authorization
      .map(auth => request.setHeader(HeaderNames.AUTHORIZATION -> auth.value))
      .getOrElse(request)
      .executeAndDeserialise[Registration]
  }

  def upsertRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .put(url"$eclRegistrationUrl/registrations")
      .withBody(Json.toJson(registration))
      .executeAndContinue

  def deleteRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .delete(url"$eclRegistrationUrl/registrations/$internalId")
      .executeAndContinue

  def getSubscriptionStatus(businessPartnerId: String)(implicit hc: HeaderCarrier): Future[EclSubscriptionStatus] =
    httpClient
      .get(url"$eclRegistrationUrl/subscription-status/SAFE/$businessPartnerId")
      .executeAndDeserialise[EclSubscriptionStatus]

  def getRegistrationValidationErrors(
    internalId: String
  )(implicit hc: HeaderCarrier): OptionT[Future, String] =
    httpClient
      .get(url"$eclRegistrationUrl/registrations/$internalId/validation-errors")
      .executeAndDeserialiseOption[String]

  def submitRegistration(internalId: String)(implicit
    hc: HeaderCarrier
  ): Future[CreateEclSubscriptionResponse] =
    httpClient
      .post(url"$eclRegistrationUrl/submit-registration/$internalId")
      .setHeader("Authorization" -> hc.authorization.get.value)
      .executeAndDeserialise[CreateEclSubscriptionResponse]

  def getSubscription(eclReference: String)(implicit hc: HeaderCarrier): Future[GetSubscriptionResponse] =
    httpClient
      .get(url"$eclRegistrationUrl/subscription/$eclReference")
      .executeAndDeserialise[GetSubscriptionResponse]

}

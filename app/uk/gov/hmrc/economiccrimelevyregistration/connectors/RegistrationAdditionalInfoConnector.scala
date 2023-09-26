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

import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataValidationErrors
import uk.gov.hmrc.economiccrimelevyregistration.models.{CreateEclSubscriptionResponse, EclSubscriptionStatus, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationAdditionalInfoConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit
  ec: ExecutionContext
) extends Logging {

  private val eclRegistrationUrl: String =
    s"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def get(internalId: String)(implicit hc: HeaderCarrier): Future[Option[RegistrationAdditionalInfo]] =
    httpClient.GET[Option[RegistrationAdditionalInfo]](
      s"$eclRegistrationUrl/registration-additional-info/$internalId"
    )

  def upsert(registration: RegistrationAdditionalInfo)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient.PUT[RegistrationAdditionalInfo, Unit](
      s"$eclRegistrationUrl/registration-additional-info",
      registration
    )

  def delete(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[Unit](
        s"$eclRegistrationUrl/registration-additional-info/$internalId"
      )
}

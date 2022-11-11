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

import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, Registration}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclRegistrationConnector @Inject() (appConfig: AppConfig, httpClient: HttpClient)(implicit ec: ExecutionContext) {

  private val eclRegistrationUrl: String =
    s"${appConfig.eclRegistrationBaseUrl}/economic-crime-levy-registration"

  def getRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Option[Registration]] =
    httpClient.GET[Option[Registration]](
      s"$eclRegistrationUrl/registrations/$internalId"
    )

  def upsertRegistration(registration: Registration)(implicit hc: HeaderCarrier): Future[Registration] =
    httpClient.PUT[Registration, Registration](
      s"$eclRegistrationUrl/registrations",
      registration
    )

  def deleteRegistration(internalId: String)(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .DELETE[HttpResponse](
        s"$eclRegistrationUrl/registrations/$internalId"
      )
      .map(_ => ())

  def getSubscriptionStatus(businessPartnerId: String)(implicit hc: HeaderCarrier): Future[EclSubscriptionStatus] =
    httpClient.GET[EclSubscriptionStatus](
      s"$eclRegistrationUrl/subscription-status/$businessPartnerId"
    )
}

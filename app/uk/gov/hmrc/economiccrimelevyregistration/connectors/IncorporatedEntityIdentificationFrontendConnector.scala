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

import play.api.i18n.MessagesApi
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, IncorporatedEntityCreateJourneyRequest, IncorporatedEntityJourneyData, ServiceNameLabels}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorporatedEntityIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) {
  private val apiUrl = s"${appConfig.incorporatedEntityIdentificationApiUrl}/incorporated-entity-identification/api"

  private val createJourneyRequest = {
    val serviceNameLabels = ServiceNameLabels()

    IncorporatedEntityCreateJourneyRequest(
      continueUrl = appConfig.grsContinueUrl,
      optServiceName = Some(serviceNameLabels.en.optServiceName),
      deskProServiceId = appConfig.appName,
      signOutUrl = appConfig.grsSignOutUrl,
      accessibilityUrl = appConfig.grsAccessibilityStatementPath,
      labels = serviceNameLabels
    )
  }

  def createLimitedCompanyJourney()(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] =
    httpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      s"$apiUrl/limited-company-journey",
      createJourneyRequest
    )

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData] =
    httpClient.GET[IncorporatedEntityJourneyData](s"$apiUrl/journey/$journeyId")
}

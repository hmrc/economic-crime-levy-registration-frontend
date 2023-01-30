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

import play.api.i18n.MessagesApi
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, ServiceNameLabels, SoleTraderEntityCreateJourneyRequest, SoleTraderEntityJourneyData}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait SoleTraderIdentificationFrontendConnector {
  def createSoleTraderJourney()(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse]

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData]
}

class SoleTraderIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends SoleTraderIdentificationFrontendConnector {
  private val apiUrl = s"${appConfig.soleTraderEntityIdentificationFrontendBaseUrl}/sole-trader-identification/api"

  def createSoleTraderJourney()(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    httpClient.POST[SoleTraderEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      s"$apiUrl/sole-trader-journey",
      SoleTraderEntityCreateJourneyRequest(
        continueUrl = appConfig.grsContinueUrl,
        businessVerificationCheck = appConfig.soleTraderBvEnabled,
        optServiceName = Some(serviceNameLabels.en.optServiceName),
        deskProServiceId = appConfig.appName,
        signOutUrl = appConfig.eclSignOutUrl,
        accessibilityUrl = appConfig.accessibilityStatementPath,
        labels = serviceNameLabels
      )
    )
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData] =
    httpClient.GET[SoleTraderEntityJourneyData](s"$apiUrl/journey/$journeyId")
}

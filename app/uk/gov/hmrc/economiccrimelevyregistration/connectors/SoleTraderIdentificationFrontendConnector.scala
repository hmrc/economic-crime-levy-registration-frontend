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
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait SoleTraderIdentificationFrontendConnector {
  def createSoleTraderJourney(mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse]

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData]
}

class SoleTraderIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends SoleTraderIdentificationFrontendConnector
    with BaseConnector {
  private val apiUrl: URL =
    url"${appConfig.soleTraderEntityIdentificationFrontendBaseUrl}/sole-trader-identification/api"

  def createSoleTraderJourney(mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    httpClient
      .post(url"$apiUrl/sole-trader-journey")
      .withBody(Json.toJson(toSoleTraderEntityCreateJourneyRequest(mode, serviceNameLabels)))
      .executeAndDeserialise[GrsCreateJourneyResponse]
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData] =
    httpClient
      .get(url"$apiUrl/journey/$journeyId")
      .executeAndDeserialise[SoleTraderEntityJourneyData]

  private def toSoleTraderEntityCreateJourneyRequest(
    mode: Mode,
    serviceNameLabels: ServiceNameLabels
  ): SoleTraderEntityCreateJourneyRequest =
    SoleTraderEntityCreateJourneyRequest(
      continueUrl = s"${appConfig.grsContinueUrl}/${mode.toString.toLowerCase}",
      businessVerificationCheck = appConfig.soleTraderBvEnabled,
      optServiceName = Some(serviceNameLabels.en.optServiceName),
      deskProServiceId = appConfig.appName,
      signOutUrl = appConfig.eclSignOutUrl,
      accessibilityUrl = appConfig.accessibilityStatementPath,
      labels = serviceNameLabels
    )
}

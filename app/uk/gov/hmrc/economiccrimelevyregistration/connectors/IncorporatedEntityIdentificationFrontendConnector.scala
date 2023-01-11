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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, IncorporatedEntityCreateJourneyRequest, IncorporatedEntityJourneyData, ServiceNameLabels}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IncorporatedEntityIdentificationFrontendConnector {
  def createLimitedCompanyJourney()(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse]
  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData]
}

class IncorporatedEntityIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends IncorporatedEntityIdentificationFrontendConnector {
  private val apiUrl =
    s"${appConfig.incorporatedEntityIdentificationFrontendUrl}/incorporated-entity-identification/api"

  def createLimitedCompanyJourney()(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    httpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      s"$apiUrl/limited-company-journey",
      IncorporatedEntityCreateJourneyRequest(
        continueUrl = appConfig.grsContinueUrl,
        businessVerificationCheck = appConfig.incorporatedEntityBvEnabled,
        optServiceName = Some(serviceNameLabels.en.optServiceName),
        deskProServiceId = appConfig.appName,
        signOutUrl = appConfig.eclSignOutUrl,
        accessibilityUrl = appConfig.accessibilityStatementPath,
        labels = serviceNameLabels
      )
    )
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData] =
    httpClient.GET[IncorporatedEntityJourneyData](s"$apiUrl/journey/$journeyId")
}

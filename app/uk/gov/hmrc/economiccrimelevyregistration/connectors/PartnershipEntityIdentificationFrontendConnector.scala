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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, PartnershipEntityCreateJourneyRequest, PartnershipEntityJourneyData, ServiceNameLabels}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnershipEntityIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) {
  private val apiUrl = s"${appConfig.partnershipEntityIdentificationApiUrl}/partnership-identification/api"

  def createLimitedLiabilityPartnershipJourney()(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    httpClient.POST[PartnershipEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      s"$apiUrl/limited-liability-partnership-journey",
      PartnershipEntityCreateJourneyRequest(
        continueUrl = appConfig.grsContinueUrl,
        businessVerificationCheck = None,
        optServiceName = Some(serviceNameLabels.en.optServiceName),
        deskProServiceId = appConfig.appName,
        signOutUrl = appConfig.grsSignOutUrl,
        accessibilityUrl = appConfig.grsAccessibilityStatementPath,
        labels = serviceNameLabels
      )
    )
  }

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData] =
    httpClient.GET[PartnershipEntityJourneyData](s"$apiUrl/journey/$journeyId")
}

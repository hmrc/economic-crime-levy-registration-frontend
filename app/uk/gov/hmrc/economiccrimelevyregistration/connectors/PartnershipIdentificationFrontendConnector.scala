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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, PartnershipEntityCreateJourneyRequest, PartnershipEntityJourneyData, ServiceNameLabels}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait PartnershipIdentificationFrontendConnector {
  def createPartnershipJourney(
    partnershipType: EntityType
  )(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse]

  def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData]
}

class PartnershipIdentificationFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends PartnershipIdentificationFrontendConnector {
  private val apiUrl = s"${appConfig.partnershipEntityIdentificationFrontendUrl}/partnership-identification/api"

  def createPartnershipJourney(
    partnershipType: EntityType
  )(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    val url: String = partnershipType match {
      case GeneralPartnership          => s"$apiUrl/general-partnership-journey"
      case ScottishPartnership         => s"$apiUrl/scottish-partnership-journey"
      case LimitedPartnership          => s"$apiUrl/limited-partnership-journey"
      case ScottishLimitedPartnership  => s"$apiUrl/scottish-limited-partnership-journey"
      case LimitedLiabilityPartnership => s"$apiUrl/limited-liability-partnership-journey"
      case e                           => throw new IllegalArgumentException(s"$e is not a valid partnership type")
    }

    httpClient.POST[PartnershipEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      url,
      PartnershipEntityCreateJourneyRequest(
        continueUrl = appConfig.grsContinueUrl,
        businessVerificationCheck = appConfig.partnershipBvEnabled,
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

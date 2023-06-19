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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{RegisteredSociety, UkLimitedCompany, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{GrsCreateJourneyResponse, IncorporatedEntityCreateJourneyRequest, IncorporatedEntityJourneyData, ServiceNameLabels}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Mode}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IncorporatedEntityIdentificationFrontendConnector {
  def createIncorporatedEntityJourney(incorporatedEntityType: EntityType, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse]
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
    s"${appConfig.incorporatedEntityIdentificationFrontendBaseUrl}/incorporated-entity-identification/api"

  def createIncorporatedEntityJourney(incorporatedEntityType: EntityType, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] = {
    val serviceNameLabels = ServiceNameLabels()

    val url: String = incorporatedEntityType match {
      case UkLimitedCompany | UnlimitedCompany => s"$apiUrl/limited-company-journey"
      case RegisteredSociety                   => s"$apiUrl/registered-society-journey"
      case e                                   => throw new IllegalArgumentException(s"$e is not a valid incorporated entity type")
    }

    httpClient.POST[IncorporatedEntityCreateJourneyRequest, GrsCreateJourneyResponse](
      url,
      IncorporatedEntityCreateJourneyRequest(
        continueUrl = s"${appConfig.grsContinueUrl}/${mode.toString.toLowerCase}",
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
